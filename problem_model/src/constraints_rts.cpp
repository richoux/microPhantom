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

Assignment::Assignment( const vector<int> variables_index,
                        double possessed_units )
	: Constraint(variables_index),
	  _possessed_units(possessed_units)
{ }

// assign_XL + assign_XR + assign_XH = possessed_X + to_produce_X
double Assignment::required_error( const vector<Variable*>& variables ) const
{
	double assigned = variables[0]->get_value()
		+ variables[1]->get_value()
		+ variables[2]->get_value();

	double own = _possessed_units + variables[3]->get_value();

	return std::abs( assigned - own );
}


/*******************
 ** Stock
 *******************/

Stock::Stock( const vector<int> variables_index,
              int heavy_error,
              int light_error,
              int ranged_error,
              double stock )
	: Constraint(variables_index),
	  _heavy_error(heavy_error),
	  _light_error(light_error),
	  _ranged_error(ranged_error),
	  _stock(stock)
{ }

// H_error*to_produce_H + L_error*to_produce_L + R_error*to_produce_R <= stock
double Stock::required_error( const vector<Variable*>& variables ) const
{
	double sum = _heavy_error * variables[0]->get_value()
	           + _light_error * variables[1]->get_value()
	           + _ranged_error * variables[2]->get_value();

	return std::max( 0., sum - _stock );
}


/**********************
 ** ProductionCapacity
 **********************/

ProductionCapacity::ProductionCapacity( const vector<int> variables_index,
                                        int nb_barracks )
	: Constraint(variables_index),
	  _nb_barracks(nb_barracks)
{ }

// to_produce_H + to_produce_L + to_produce_R <= barracks production capacity
double ProductionCapacity::required_error( const vector<Variable*>& variables ) const
{
	double sum = variables[0]->get_value() + variables[1]->get_value() + variables[2]->get_value();

	return std::max( 0., sum - _nb_barracks );
}
