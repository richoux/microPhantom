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

#include <vector>
#include <numeric>

#include "ghost/variable.hpp"
#include "ghost/solver.hpp"

#include "constraints_rts.hpp"
#include "obj_rts.hpp"
#include "phi_function.hpp"

#include "randutils.hpp"

using namespace std;

int main( int argc, char *argv[] )
{
	int solver_type = std::stoi( argv[1] );
	std::ifstream infile( argv[2] );
	// std::ofstream outfile;
	// outfile.open( "run.log", std::ios::app );

	int nb_samples = std::stoi( argv[3] );
	int time;
	int nb_barracks, min_distance_resource_base, max_distance_resource_base;
	int no_initial_base, no_initial_barracks;
	int resources, initial_resources, enemy_resources_loss;
	int worker_move_time, worker_harvest_time, worker_return_time, harvest_amount;
	int base_cost, barracks_cost, heavy_cost, ranged_cost, light_cost;
	int my_heavy_units, my_light_units, my_ranged_units;
	int initial_enemy_worker;
	int observed_enemy_worker, observed_enemy_heavy, observed_enemy_light, observed_enemy_ranged;
	int observed_enemy_worker_in_total, observed_enemy_heavy_in_total, observed_enemy_light_in_total, observed_enemy_ranged_in_total;

	infile >> time >> nb_barracks >> min_distance_resource_base >> max_distance_resource_base >> no_initial_base >> no_initial_barracks;
	infile >> resources >> initial_resources >> enemy_resources_loss;
	infile >> worker_move_time >> worker_harvest_time >> worker_return_time >> harvest_amount;
	infile >> base_cost >> barracks_cost >> heavy_cost >> light_cost >> ranged_cost;
	infile >> my_heavy_units >> my_light_units >> my_ranged_units;
	infile >> initial_enemy_worker >> observed_enemy_worker >> observed_enemy_heavy >> observed_enemy_light >> observed_enemy_ranged;
	infile >> observed_enemy_worker_in_total >> observed_enemy_heavy_in_total >> observed_enemy_light_in_total >> observed_enemy_ranged_in_total;

	// outfile << "######################\n" << "Time: " << time << "\n";
	// outfile << observed_enemy_heavy << "/" << observed_enemy_heavy_in_total << ", "
	//         << observed_enemy_light << "/" << observed_enemy_light_in_total << ", "
	//         << observed_enemy_ranged << "/" << observed_enemy_ranged_in_total << "\n";
	
	observed_enemy_worker = std::max( initial_enemy_worker, observed_enemy_worker );

	// Estimate how much resources the opponent has.
	double mean_distance;
	if( min_distance_resource_base != -1 )
		mean_distance = static_cast<double>( min_distance_resource_base + max_distance_resource_base ) / 2;
	else
		mean_distance = 20.0; // let's consider resources are far away

	// outfile << "distances (min, mean, max): (" << min_distance_resource_base << ", " << mean_distance << ", " << max_distance_resource_base << "), move time: " << worker_move_time << ", harvest time: " << worker_harvest_time << ", return time: " << worker_return_time << "\n";
	
	// 20 * ( initial_enemy_worker - 1 ) is to express a penalty when there are more than one worker: they tend to hinder each other.
	int gathered_resources = harvest_amount * initial_enemy_worker * ( time / ( mean_distance * worker_move_time * 2 + worker_harvest_time + worker_return_time + ( 20 * ( initial_enemy_worker - 1 ) ) ) );
	int estimated_cumulated_resources = initial_resources + gathered_resources;
	int value_enemy_army = observed_enemy_heavy * heavy_cost + observed_enemy_light * light_cost + observed_enemy_ranged * ranged_cost;
	int estimated_remaining_resources = std::max( 0, estimated_cumulated_resources - ( no_initial_base * base_cost + no_initial_barracks * barracks_cost + enemy_resources_loss + value_enemy_army ) );

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
	int min_cost = heavy_cost;
	if( light_cost < min_cost )
	{
		if( ranged_cost < light_cost )
			min_cost = ranged_cost;
		else
			min_cost = light_cost;
	}
	else
		if( ranged_cost < min_cost )
			min_cost = ranged_cost;

	// Enemy units currently seen count double. Enemy units unseen but we saw before (so dead or returned under the fog) count simple.
	observed_enemy_heavy_in_total -= observed_enemy_heavy;
	observed_enemy_light_in_total -= observed_enemy_light;
	observed_enemy_ranged_in_total -= observed_enemy_ranged;
	
	// +1 to each unit type to never have a probability = 0 of producing any type of unit.
	int total = 3 + 2 * ( observed_enemy_heavy + observed_enemy_light + observed_enemy_ranged ) + ( observed_enemy_heavy_in_total + observed_enemy_light_in_total + observed_enemy_ranged_in_total );
	auto distribution = { ( 1 + 2 * observed_enemy_heavy + observed_enemy_heavy_in_total ) * 100.0 / total,
	                      ( 1 + 2 * observed_enemy_light + observed_enemy_light_in_total ) * 100.0 / total,
	                      ( 1 + 2 * observed_enemy_ranged + observed_enemy_ranged_in_total ) * 100.0 / total };
	// outfile << "Distribution: "
	//         << ( 1 + 2 * observed_enemy_heavy + observed_enemy_heavy_in_total ) * 100.0 / total << ", "
	//         << ( 1 + 2 * observed_enemy_light + observed_enemy_light_in_total ) * 100.0 / total << ", "
	//         << ( 1 + 2 * observed_enemy_ranged + observed_enemy_ranged_in_total ) * 100.0 / total  << "\nSamples:\n";

	randutils::mt19937_rng rng;
	vector< vector<int> > samples;
	
	for( int counter = 0; counter < nb_samples; ++counter )
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
				if( estimated_resources >= heavy_cost )
				{
					++number_estimated_heavy;
					estimated_resources -= heavy_cost;
				}
				break;
			case 1: // light
				if( estimated_resources >= light_cost )
				{
					++number_estimated_light;
					estimated_resources -= light_cost;
				}
				break;
			case 2: // ranged
				if( estimated_resources >= ranged_cost )
				{
					++number_estimated_ranged;
					estimated_resources -= ranged_cost;
				}
				break;
			default:
				break;
			}
		}
		// outfile << number_estimated_heavy + observed_enemy_heavy << ", " << number_estimated_light + observed_enemy_light << ", " << number_estimated_ranged + observed_enemy_ranged << "\n";
		samples.push_back( { number_estimated_heavy + observed_enemy_heavy, number_estimated_light + observed_enemy_light, number_estimated_ranged + observed_enemy_ranged } );
	}
			
	vector<Variable> variables;
	
	// Our units assigned to enemy units
	variables.push_back( Variable( "Heavy assigned to heavy", "assign_Hh", 0, 20 + my_heavy_units ) ); //0
	variables.push_back( Variable( "Light assigned to heavy", "assign_Lh", 0, 20 + my_light_units ) ); 
	variables.push_back( Variable( "Ranged assigned to heavy", "assign_Rh", 0, 20 + my_ranged_units ) );

	variables.push_back( Variable( "Heavy assigned to light", "assign_Hl", 0, 20 + my_heavy_units ) ); //3
	variables.push_back( Variable( "Light assigned to light", "assign_Ll", 0, 20 + my_light_units ) ); 
	variables.push_back( Variable( "Ranged assigned to light", "assign_Rl", 0, 20 + my_ranged_units ) );

	variables.push_back( Variable( "Heavy assigned to ranged", "assign_Hr", 0, 20 + my_heavy_units ) ); //6
	variables.push_back( Variable( "Light assigned to ranged", "assign_Lr", 0, 20 + my_light_units ) ); 
	variables.push_back( Variable( "Ranged assigned to ranged", "assign_Rr", 0, 20 + my_ranged_units ) );

	variables.push_back( Variable( "Heavy to produce", "to_prod_H", 0, 20 ) ); //9
	variables.push_back( Variable( "Light to produce", "to_prod_L", 0, 20 ) ); 
	variables.push_back( Variable( "Ranged to produce", "to_prod_R", 0, 20 ) ); 

	vector< reference_wrapper<Variable> > variables_heavy{ variables[0], variables[3], variables[6], variables[9] };
	vector< reference_wrapper<Variable> > variables_light{ variables[1], variables[4], variables[7], variables[10] };
	vector< reference_wrapper<Variable> > variables_ranged{ variables[2], variables[5], variables[8], variables[11] };
	vector< reference_wrapper<Variable> > variables_stock( variables.begin() + 9, variables.end() );

	shared_ptr<Constraint> assign_heavy = make_shared<Assignment>( variables_heavy, my_heavy_units );
	shared_ptr<Constraint> assign_light = make_shared<Assignment>( variables_light, my_light_units );
	shared_ptr<Constraint> assign_ranged = make_shared<Assignment>( variables_ranged, my_ranged_units );
	shared_ptr<Constraint> stock = make_shared<Stock>( variables_stock, heavy_cost, light_cost, ranged_cost, resources );
	shared_ptr<Constraint> capacity = make_shared<ProductionCapacity>( variables_stock, nb_barracks );

	vector< shared_ptr<Constraint> > constraints = { assign_heavy, assign_light, assign_ranged, stock, capacity };

	std::function<double(double)> phi_callback;
	if( solver_type == 2)
		phi_callback = pessimistic();
	else if( solver_type == 1)
		phi_callback = optimistic();
	else
		phi_callback = identity();

	// Coefficients:
	// H vs H, L vs H, R vs H
	// H vs L, L vs L, R vs L
	// H vs R, L vs R, R vs R
	shared_ptr<Objective> obj = make_shared<BestComposition>( vector<double>{ 1.   , 0.374, 1.564,
	                                                                          2.675, 1.   , 0.472,
	                                                                          0.639, 2.119, 1. },
	                                                          samples,
	                                                          phi_callback );

	Solver solver_p( variables, constraints, obj );

	vector<int>solution( variables.size(), 0 );
	double cost_p = 0.;

	/*
	 * POAdaptive is waiting for 6 lines
	 * The 3 first lines are dummy lines, just for debug
	 * The 3 last lines contain necessary information: number of heavy/ranged/light units to produce
	 */
	// cout << "Solve ..." << "\n";
	// cout << solver_p.solve( cost_p, solution, 10000, 100000 ) << " : " << cost_p << " / " << obj->cost( variables ) << "\n";
	solver_p.solve( cost_p, solution, 10000, 100000 );
	cout << solution[9] << "\n" << solution[10] << "\n" << solution[11] << "\n";

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
