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
	std::ifstream infile( argv[1] );
	std::ofstream outfile;
	outfile.open( "run.log", std::ios::app );

	int nb_samples = std::stoi( argv[2] );
	int nb_barracks, resources;
	int heavy_cost, ranged_cost, light_cost;
	int my_heavy_units, my_light_units, my_ranged_units;
	int initial_enemy_worker;
	int observed_enemy_worker, observed_enemy_heavy, observed_enemy_light, observed_enemy_ranged;

	infile >> nb_barracks;
	infile >> resources;
	infile >> heavy_cost;
	infile >> light_cost;
	infile >> ranged_cost;
	infile >> my_heavy_units;
	infile >> my_light_units;
	infile >> my_ranged_units;
	infile >> initial_enemy_worker;
	infile >> observed_enemy_worker;
	infile >> observed_enemy_heavy;
	infile >> observed_enemy_light;
	infile >> observed_enemy_ranged;

	observed_enemy_worker = std::max( initial_enemy_worker, observed_enemy_worker );
	
	vector<Variable> variables;
	auto distribution = { 1 + observed_enemy_heavy, 1 + observed_enemy_light, 1 + observed_enemy_ranged };
	randutils::mt19937_rng rng;
	rng.variate< int, std::discrete_distribution >( distribution );
	//discrete_distribution<> distribution( { 1 + observed_enemy_heavy, 1 + observed_enemy_light, 1 + observed_enemy_ranged } );
	
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
	shared_ptr<Constraint> stock = make_shared<Stock>( variables_stock, heavy_cost, light_cost, ranged_cost, nb_barracks, resources );
	shared_ptr<Constraint> capacity = make_shared<ProductionCapacity>( variables_stock, nb_barracks );

	vector< shared_ptr<Constraint> > constraints = { assign_heavy, assign_light, assign_ranged, stock, capacity };

#if defined(PESSIMISTIC)
	auto phi_callback = pessimistic();
#elif defined(OPTIMISTIC)
	auto phi_callback = optimistic();
#else
	auto phi_callback = identity();
#endif

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
	cout << solution[9] << "\n" << solution[11] << "\n" << solution[10] << "\n";

	outfile << solution[0] << ", "
	        << solution[1] << ", "
	        << solution[2] << ", "
	        << solution[3] << ", "
	        << solution[4] << ", "
	        << solution[5] << ", "
	        << solution[6] << ", "
	        << solution[7] << ", "
	        << solution[8] << ", "
	        << solution[9] << ", "
	        << solution[10] << ", "
	        << solution[11] << "\n";
	
	return 0;
}
