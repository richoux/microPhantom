/*
 * microPhantom is an AI bot playing microRTS.
 * It uses GHOST, a Constraint Programming toolkit, to design and solve combinatorial problems for
 * all decision-making behaviors. Please visit https://github.com/richoux/microPhantom for further information.
 * 
 * Copyright (C) 2020 Florian Richoux
 *
 * This file is part of microPhantom.
 * microPhantom is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * microPhantom is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with microPhantom. If not, see http://www.gnu.org/licenses/.
 */


#include <unistd.h>
#include <ios>
#include <iostream>
#include <fstream>
#include <string>
#include <memory>
#include <random>
#include <algorithm>
#include <arpa/inet.h>
#include <sys/socket.h>

#include <vector>
#include <numeric>

#include "ghost/solver.hpp"
#include "builder.hpp"
#include "randutils.hpp"
#include "microphantom.pb.h"

using namespace std;
using namespace ghost;
using namespace std::literals::chrono_literals;

int main( int argc, char *argv[] )
{
	//cout << "Lancement du programme\n";

	GOOGLE_PROTOBUF_VERIFY_VERSION;
	//cout << "Pas d'erreur de version protobuf\n";

	int sock = 0;
	struct sockaddr_in serv_addr;
	int port = 1085;
	
	if( ( sock = socket( AF_INET, SOCK_STREAM, 0 ) ) < 0 )
	{
		//cout << "\n Socket creation error \n";
		exit( EXIT_FAILURE );
	}

	//cout << "C++ Socket créé\n";

	serv_addr.sin_family = AF_INET;
	serv_addr.sin_port = htons( port );
	
	// Convert IPv4 and IPv6 addresses from text to binary form
	if( inet_pton( AF_INET, "127.0.0.1", &serv_addr.sin_addr ) <= 0 ) 
	{
		//cout << "\nInvalid address/ Address not supported \n";
		exit( EXIT_FAILURE );
	}

	//cout << "C++ adresse affectée au client \n";

	if( connect( sock, (struct sockaddr *)&serv_addr, sizeof( serv_addr ) ) < 0 )
	{
		//cout << "\nConnection Failed \n";
		exit( EXIT_FAILURE );
	}

	//cout << "C++ client connecté \n";
	GameStateBuffer game_state;
	
	char* buffer = new char[1024];
	recv( sock, buffer, 1024, 0 );
	game_state.ParseFromString( buffer );

	//cout << "C++ réception des données du serveur\n";

	// outfile << "######################\n" << "Time: " << time << "\n";
	// outfile << observed_enemy_heavy << "/" << observed_enemy_heavy_in_total << ", "
	//         << observed_enemy_light << "/" << observed_enemy_light_in_total << ", "
	//         << observed_enemy_ranged << "/" << observed_enemy_ranged_in_total << "\n";

	int observed_enemy_worker = std::max( game_state.initial_enemy_worker(), game_state.observed_enemy_worker() );

	// Estimate how much resources the opponent has.
	double mean_distance;
	if( game_state.min_distance_resource_base() != -1 )
		mean_distance = static_cast<double>( game_state.min_distance_resource_base() + game_state.max_distance_resource_base() ) / 2;
	else
		mean_distance = 20.0; // let's consider resources are far away

	// outfile << "distances (min, mean, max): (" << min_distance_resource_base << ", " << mean_distance << ", " << max_distance_resource_base << "), move time: " << worker_move_time << ", harvest time: " << worker_harvest_time << ", return time: " << worker_return_time << "\n";
	
	// 20 * ( initial_enemy_worker - 1 ) is to express a penalty when there are more than one worker: they tend to hinder each other.
	int gathered_resources = game_state.harvest_amount() * game_state.initial_enemy_worker() * ( game_state.time() / ( mean_distance * game_state.worker_move_time() * 2 + game_state.worker_harvest_time() + game_state.worker_return_time() + ( 20 * ( game_state.initial_enemy_worker() - 1 ) ) ) );
	int estimated_cumulated_resources = game_state.initial_resources() + gathered_resources;
	int value_enemy_army = game_state.observed_enemy_heavy() * game_state.heavy_cost() + game_state.observed_enemy_light() * game_state.light_cost() + game_state.observed_enemy_ranged() * game_state.ranged_cost();
	int estimated_remaining_resources = std::max( 0, estimated_cumulated_resources - ( game_state.no_initial_base() * game_state.base_cost() + game_state.no_initial_barracks() * game_state.barracks_cost() + game_state.enemy_resources_loss() + value_enemy_army ) );

	// if( no_initial_base )
	// 	outfile << "No initial base (" << base_cost << "), ";
	// else
	// 	outfile << "Has base (" << base_cost << "), ";

	// if( no_initial_barracks )
	// 	outfile << "no initial barracks (" << barracks_cost << ").\n";
	// else
	// 	outfile << "has barracks (" << barracks_cost << ").\n";

	// outfile << "Enemy loss: " << enemy_resources_loss << "\n";
	// outfile << "Resources estimations: gathered=" << gathered_resources << ", initial=" << initial_resources << ", value army=" << value_enemy_army << ", remaining=" << estimated_remaining_resources << "\n";
	
	// after estimating how much resources we haven't seen used from the opponent, we need to estimate how the opponent spent it!
	int min_cost = game_state.heavy_cost();
	if( game_state.light_cost() < min_cost )
	{
		if( game_state.ranged_cost() < game_state.light_cost() )
			min_cost = game_state.ranged_cost();
		else
			min_cost = game_state.light_cost();
	}
	else
		if( game_state.ranged_cost() < min_cost )
			min_cost = game_state.ranged_cost();

	// Enemy units currently seen count double. Enemy units unseen but we saw before (so dead or returned under the fog) count simple.
	int observed_enemy_heavy_in_total = game_state.observed_enemy_heavy_in_total() - game_state.observed_enemy_heavy();
	int observed_enemy_light_in_total = game_state.observed_enemy_light_in_total() - game_state.observed_enemy_light();
	int observed_enemy_ranged_in_total = game_state.observed_enemy_ranged_in_total() - game_state.observed_enemy_ranged();
	
	// +1 to each unit type to never have a probability = 0 of producing any type of unit.
	int total = 3 + 2 * ( game_state.observed_enemy_heavy() + game_state.observed_enemy_light() + game_state.observed_enemy_ranged() ) + ( observed_enemy_heavy_in_total + observed_enemy_light_in_total + observed_enemy_ranged_in_total );
	auto distribution = { ( 1 + 2 * game_state.observed_enemy_heavy() + observed_enemy_heavy_in_total ) * 100.0 / total,
	                      ( 1 + 2 * game_state.observed_enemy_light() + observed_enemy_light_in_total ) * 100.0 / total,
	                      ( 1 + 2 * game_state.observed_enemy_ranged() + observed_enemy_ranged_in_total ) * 100.0 / total };
	// outfile << "Distribution: "
	//         << ( 1 + 2 * observed_enemy_heavy + observed_enemy_heavy_in_total ) * 100.0 / total << ", "
	//         << ( 1 + 2 * observed_enemy_light + observed_enemy_light_in_total ) * 100.0 / total << ", "
	//         << ( 1 + 2 * observed_enemy_ranged + observed_enemy_ranged_in_total ) * 100.0 / total  << "\nSamples:\n";

	randutils::mt19937_rng rng;
	vector< vector<int> > samples;
	
	for( int counter = 0; counter < game_state.nb_samples(); ++counter )
	{
		int estimated_resources = estimated_remaining_resources;
		int unit_produced = -1;
		int number_estimated_heavy = 0;
		int number_estimated_light = 0;
		int number_estimated_ranged = 0;
		
		// while the opponent can produce something, we consider he or she will
		while( estimated_resources >= min_cost )
		{
			unit_produced	= rng.variate< int, std::discrete_distribution >( distribution );
			switch( unit_produced )
			{
			case 0: // heavy
				if( estimated_resources >= game_state.heavy_cost() )
				{
					++number_estimated_heavy;
					estimated_resources -= game_state.heavy_cost();
				}
				break;
			case 1: // light
				if( estimated_resources >= game_state.light_cost() )
				{
					++number_estimated_light;
					estimated_resources -= game_state.light_cost();
				}
				break;
			case 2: // ranged
				if( estimated_resources >= game_state.ranged_cost() )
				{
					++number_estimated_ranged;
					estimated_resources -= game_state.ranged_cost();
				}
				break;
			default:
				break;
			}
		}
		// outfile << number_estimated_heavy + observed_enemy_heavy << ", " << number_estimated_light + observed_enemy_light << ", " << number_estimated_ranged + observed_enemy_ranged << "\n";
		samples.push_back( { number_estimated_heavy + game_state.observed_enemy_heavy(), number_estimated_light + game_state.observed_enemy_light(), number_estimated_ranged + game_state.observed_enemy_ranged() } );
	}
			
	Builder builder( game_state.solver_type(),
	                 game_state.my_heavy_units(),
	                 game_state.my_light_units(),
	                 game_state.my_ranged_units(),
	                 samples,
	                 game_state.resources(),
	                 game_state.nb_barracks(),
	                 game_state.heavy_cost(),
	                 game_state.light_cost(),
	                 game_state.ranged_cost() );
	Options options;
	options.parallel_runs = true;
	options.number_threads = std::max( 2, options.number_threads / 2 );
	
	Solver solver_p( builder );

	vector<int> vec_solution;
	double cost_p;

	/*
	 * POAdaptive is waiting for 6 lines
	 * The 3 first lines are dummy lines, just for debug
	 * The 3 last lines contain necessary information: number of heavy/ranged/light units to produce
	 */
	// cout << "Solve ..." << "\n";
	// cout << solver_p.solve( cost_p, solution, 10000, 100000 ) << " : " << cost_p << " / " << obj->cost( variables ) << "\n";
	solver_p.solve( cost_p, vec_solution, 90ms, options );

	//cout << "C++ solution trouvée\n";

	SolutionBuffer solution;

	solution.set_number_heavy( vec_solution[9] );
	solution.set_number_light( vec_solution[10] );
	solution.set_number_ranged( vec_solution[11] );

	auto size = solution.ByteSizeLong();
	char* array = new char[size];
	solution.SerializeToArray( array, size );
	
	send( sock, (const char*)array, size, 0 );

	//cout << "C++ envoie de la solution au serveur\n";

	google::protobuf::ShutdownProtobufLibrary();

	// outfile << "Solution: "
	//         << solution[0] << ", "
	//         << solution[1] << ", "
	//         << solution[2] << ", "
	//         << solution[3] << ", "
	//         << solution[4] << ", "
	//         << solution[5] << ", "
	//         << solution[6] << ", "
	//         << solution[7] << ", "
	//         << solution[8] << ", "
	//         << solution[9] << ", "
	//         << solution[10] << ", "
	//         << solution[11] << "\n\n";
	
	return 0;
}
