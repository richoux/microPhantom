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

#include "builder.hpp"
#include "constraints_rts.hpp"
#include "obj_rts.hpp"
#include "phi_function.hpp"

Builder::Builder( int solver_type,
                  int my_heavy_units,
                  int my_light_units,
                  int my_ranged_units,
                  const vector<vector<int>>& samples,
                  int resources,
                  int nb_barracks,
                  int heavy_cost,
                  int light_cost,
                  int ranged_cost )
	: _solver_type(solver_type),
	  _my_heavy_units(my_heavy_units),
	  _my_light_units(my_light_units),
	  _my_ranged_units(my_ranged_units),
	  _samples(samples),
	  _resources(resources),
	  _nb_barracks(nb_barracks),
	  _heavy_cost(heavy_cost),
	  _light_cost(light_cost),
	  _ranged_cost(ranged_cost)
{ }


void Builder::declare_variables()
{
	//Heavy assigned to heavy
	variables.emplace_back( 0, 20 + _my_heavy_units, "assign_Hh" ); //0
	//Light assigned to heavy
	variables.emplace_back( 0, 20 + _my_light_units, "assign_Lh" );
	//Ranged assigned to heavy
	variables.emplace_back( 0, 20 + _my_ranged_units, "assign_Rh" );

	//Heavy assigned to light
	variables.emplace_back( 0, 20 + _my_heavy_units, "assign_Hl" ); //3
	//Light assigned to light
	variables.emplace_back( 0, 20 + _my_light_units, "assign_Ll" );
	//Ranged assigned to light
	variables.emplace_back( 0, 20 + _my_ranged_units, "assign_Rl" );

	//Heavy assigned to ranged
	variables.emplace_back( 0, 20 + _my_heavy_units, "assign_Hr" ); //6
	//Light assigned to ranged
	variables.emplace_back( 0, 20 + _my_light_units, "assign_Lr" );
	//Ranged assigned to ranged
	variables.emplace_back( 0, 20 + _my_ranged_units, "assign_Rr" );

	//Heavy to produce
	variables.emplace_back( 0, 20, "to_prod_H" ); //9
  //Light to produce	
	variables.emplace_back( 0, 20, "to_prod_L" );
	//Ranged to produce
	variables.emplace_back( 0, 20, "to_prod_R" );
}

void Builder::declare_constraints()
{
	constraints.push_back( make_shared<Assignment>( std::vector<int>{0, 3, 6, 9}, _my_heavy_units ) );
	constraints.push_back( make_shared<Assignment>( std::vector<int>{1, 4, 7, 10}, _my_light_units ) );
	constraints.push_back( make_shared<Assignment>( std::vector<int>{2, 5, 8, 11}, _my_ranged_units ) );
	constraints.push_back( make_shared<Stock>( std::vector<int>{9, 10, 11}, _heavy_cost, _light_cost, _ranged_cost, _resources ) );
	constraints.push_back( make_shared<ProductionCapacity>( std::vector<int>{9, 10, 11}, _nb_barracks ) );
}

void Builder::declare_objective()
{
	std::function<double(double)> phi_callback;
	if( _solver_type == 2)
		phi_callback = pessimistic();
	else if( _solver_type == 1)
		phi_callback = optimistic();
	else
		phi_callback = identity();

	// Coefficients:
	// H vs H, L vs H, R vs H
	// H vs L, L vs L, R vs L
	// H vs R, L vs R, R vs R
	objective = make_shared<BestComposition>( variables,
		                                        vector<double>{ 1.   , 0.374, 1.564,
	                                                          2.675, 1.   , 0.472,
	                                                          0.639, 2.119, 1. },
	                                          _samples,
	                                          phi_callback );
}
