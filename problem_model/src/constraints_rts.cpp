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


#include <iostream>
#include <algorithm>
#include <exception>

#include "constraints_rts.hpp"

using namespace std;
using namespace ghost;

/*****************
 ** Assignment
 *****************/

Assignment::Assignment( const vector< reference_wrapper<Variable> >& variables,
                        double possessed_units )
	: Constraint(variables),
	  _possessed_units(possessed_units)
{ }

// assign_XL + assign_XR + assign_XH = possessed_X + to_produce_X
double Assignment::required_cost() const
{
	double assigned = variables[0].get().get_value()
		+ variables[1].get().get_value()
		+ variables[2].get().get_value();

	double own = _possessed_units + variables[3].get().get_value();

	return std::abs( assigned - own );
}


/*******************
 ** Stock
 *******************/

Stock::Stock( const vector<reference_wrapper< Variable> >& variables,
              int heavy_cost,
              int light_cost,
              int ranged_cost,
              int nb_barracks,
              double stock )
	: Constraint(variables),
	  _heavy_cost(heavy_cost),
	  _light_cost(light_cost),
	  _ranged_cost(ranged_cost),
	  _nb_barracks(nb_barracks),
	  _stock(stock)
{ }

// H_cost*to_produce_H + L_cost*to_produce_L + R_cost*to_produce_R <= stock
double Stock::required_cost() const
{
	double sum = _heavy_cost * variables[0].get().get_value()
	           + _light_cost * variables[1].get().get_value()
	           + _ranged_cost * variables[2].get().get_value();

	return std::max( 0., sum - ( _stock + _nb_barracks ) );
}


/**********************
 ** ProductionCapacity
 **********************/

ProductionCapacity::ProductionCapacity( const vector<reference_wrapper< Variable> >& variables,
                                        int nb_barracks )
	: Constraint(variables),
	  _nb_barracks(nb_barracks)
{ }

// to_produce_H + to_produce_L + to_produce_R <= barracks production capacity
double ProductionCapacity::required_cost() const
{
	double sum = variables[0].get().get_value() + variables[1].get().get_value() + variables[2].get().get_value();

	return std::max( 0., sum - _nb_barracks );
}
