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

#include <vector>
#include <numeric>

#include "ghost/variable.hpp"
#include "ghost/solver.hpp"

#include "constraints_rts.hpp"
#include "obj_rts.hpp"
#include "phi_function.hpp"

using namespace std;

int main( int argc, char *argv[] )
{
	std::ifstream infile(argv[1]);

	std::ofstream outfile;
	outfile.open("run.log", std::ios::app);

	int nb_samples = std::stoi(argv[2]);
	int sampled_worker, sampled_heavy, sampled_range, sampled_light;
	int heavyCost, rangeCost, lightCost;
	int nb_barracks;
	int resources, my_heavy_units, my_range_units, my_light_units;

	std::vector<std::vector<int>> samples;

	for(int i = 0; i < nb_samples; ++i)
	{
		infile >> sampled_worker >> sampled_heavy >> sampled_range >> sampled_light;
		std::vector<int> tmp { sampled_worker, sampled_heavy, sampled_range, sampled_light };
		samples.push_back( tmp );
	}

	infile >> heavyCost;
	infile >> rangeCost;
	infile >> lightCost;
	infile >> nb_barracks;
	infile >> my_heavy_units;
	infile >> my_range_units;
	infile >> my_light_units;
	infile >> resources;

	// cout << "My units H/R/L: " << my_heavy_units << ", " << my_range_units << ", " << my_light_units << "\n"
	//      << "Resources: " << resources << "\n";

	//outfile << "assign_Lh, assign_Hh, assign_Rh, assign_Ll, assign_Hl, assign_Rl, assign_Lr, assign_Hr, assign_Rr, to_prod_H, to_prod_L, to_prod_R\n//////////////////////////////////////////\n\n";

	
	vector<Variable> variables;

	// Our units assigned to enemy units
	variables.push_back( Variable( "Light assigned to heavy", "assign_Lh", 0, 20 + my_light_units ) ); //0
	variables.push_back( Variable( "Heavy assigned to heavy", "assign_Hh", 0, 20 + my_heavy_units ) );
	variables.push_back( Variable( "Range assigned to heavy", "assign_Rh", 0, 20 + my_range_units ) );

	variables.push_back( Variable( "Light assigned to light", "assign_Ll", 0, 20 + my_light_units ) ); //3
	variables.push_back( Variable( "Heavy assigned to light", "assign_Hl", 0, 20 + my_heavy_units ) );
	variables.push_back( Variable( "Range assigned to light", "assign_Rl", 0, 20 + my_range_units ) );

	variables.push_back( Variable( "Light assigned to range", "assign_Lr", 0, 20 + my_light_units ) ); //6
	variables.push_back( Variable( "Heavy assigned to range", "assign_Hr", 0, 20 + my_heavy_units ) );
	variables.push_back( Variable( "Range assigned to range", "assign_Rr", 0, 20 + my_range_units ) );

	variables.push_back( Variable( "Heavy to produce", "to_prod_H", 0, 20 ) ); //9
	variables.push_back( Variable( "Light to produce", "to_prod_L", 0, 20 ) ); 
	variables.push_back( Variable( "Range to produce", "to_prod_R", 0, 20 ) ); 

	vector< reference_wrapper<Variable> > variables_heavy{ variables[1], variables[4], variables[7], variables[9] };
	vector< reference_wrapper<Variable> > variables_light{ variables[0], variables[3], variables[6], variables[10] };
	vector< reference_wrapper<Variable> > variables_range{ variables[2], variables[5], variables[8], variables[11] };
	vector< reference_wrapper<Variable> > variables_stock( variables.begin() + 9, variables.end() );

	shared_ptr<Constraint> assign_heavy = make_shared<Assignment>( variables_heavy, my_heavy_units );
	shared_ptr<Constraint> assign_light = make_shared<Assignment>( variables_light, my_light_units );
	shared_ptr<Constraint> assign_range = make_shared<Assignment>( variables_range, my_range_units );
	shared_ptr<Constraint> stock = make_shared<Stock>( variables_stock, heavyCost, lightCost, rangeCost, nb_barracks, resources );
	shared_ptr<Constraint> capacity = make_shared<ProductionCapacity>( variables_stock, nb_barracks );

	vector< shared_ptr<Constraint> > constraints = { assign_heavy, assign_light, assign_range, stock, capacity };

#if defined(PESSIMISTIC)
	auto phi_callback = pessimistic();
#elif defined(OPTIMISTIC)
	auto phi_callback = optimistic();
#else
	auto phi_callback = identity();
#endif

	// Coefficients:
	// L vs H, H vs H, R vs H
	// L vs L, H vs L, R vs L
	// L vs R, H vs R, R vs R
	shared_ptr<Objective> obj = make_shared<BestComposition>( vector<double>{ 0.374, 1.   , 1.564,
	                                                                          1.   , 2.675, 0.472,
	                                                                          2.119, 0.639, 1. },
	                                                          samples,
	                                                          phi_callback );

	Solver solver_p( variables, constraints, obj );

	vector<int>solution( variables.size(), 0 );
	double cost_p = 0.;

	/*
	 * POAdaptive is waiting for 6 lines
	 * The 3 first lines are dummy lines, just for debug
	 * The 3 last lines contain necessary information: number of heavy/range/light units to produce
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
