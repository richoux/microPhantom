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
#include <memory>

#include "ghost/model_builder.hpp"

using namespace std;
using namespace ghost;

class Builder : public ModelBuilder
{
	int _solver_type;
	int _my_heavy_units;
	int _my_light_units;
	int _my_ranged_units;
	vector<vector<int>> _samples;
	int _resources;
	int _nb_barracks;
	int _heavy_cost;
	int _light_cost;
	int _ranged_cost;

public:
	Builder( int solver_type,
	         int my_heavy_units,
	         int my_light_units,
	         int my_ranged_units,
	         const vector<vector<int>>& samples,
	         int resources,
	         int nb_barracks,
	         int heavy_cost,
	         int light_cost,
	         int ranged_cost );
	
	void declare_variables() override;
	void declare_constraints() override;
	void declare_objective() override;
};
