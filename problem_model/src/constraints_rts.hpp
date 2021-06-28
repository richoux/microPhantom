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


#pragma once

#include <vector>

#include "ghost/constraint.hpp"
#include "ghost/variable.hpp"

using namespace std;
using namespace ghost;

class Assignment : public Constraint
{
	double _possessed_units;

	double required_error( const vector<Variable*>& variables ) const override;

public:
	Assignment( const vector<int> variables_index,
	            double rhs );
};

class Stock : public Constraint
{
	int _heavy_error, _light_error, _ranged_error;
	double _stock;

	double required_error( const vector<Variable*>& variables ) const override;

public:
	Stock( const vector<int> variables_index,
	       int heavy_error,
	       int light_error,
	       int ranged_error,
	       double stock );
};

class ProductionCapacity : public Constraint
{
	int _nb_barracks;

	double required_error( const vector<Variable*>& variables ) const override;

public:
	ProductionCapacity( const vector<int> variables_index,
	                    int nb_barracks );
};
