/*
 * GHOST (General meta-Heuristic Optimization Solving Tool) is a C++ library
 * designed to help developers to model and implement optimization problem
 * solving. It contains a meta-heuristic solver aiming to solve any kind of
 * combinatorial and optimization real-time problems represented by a CSP/COP/CFN.
 *
 * GHOST has been first developped to help making AI for the RTS game
 * StarCraft: Brood war, but can be used for any kind of applications where
 * solving combinatorial and optimization problems within some tenth of
 * milliseconds is needed. It is a generalization of the Wall-in project.
 * Please visit https://github.com/richoux/GHOST for further information.
 *
 * Copyright (C) 2014-2021 Florian Richoux
 *
 * This file is part of GHOST.
 * GHOST is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * GHOST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with GHOST. If not, see http://www.gnu.org/licenses/.
 */

#pragma once

#include <memory>
#include <algorithm>

#include "print.hpp"

namespace ghost
{
	//! Options is a structure containing all optional arguments for Solver::solve.
	struct Options
	{
		bool custom_starting_point; //!< To force starting the search on a custom variables assignment.
		bool resume_search; //!< Allowing stop-and-resume computation.
		bool parallel_runs; //<! To enable parallel runs of the solver. Using all avaiable cores (including hyper-threaded cores) if number_threads is not specified.
		int number_threads; //<! Number of threads the solver will use for the search.
		std::shared_ptr<Print> print; //!< Allowing custom solution print (by derivating a class from ghost::Print)
		int tabu_time_local_min; //!< Number of local moves a variable of a local minimum is marked tabu.
		int tabu_time_selected; //!< Number of local moves a selected variable is marked tabu.
		int reset_threshold; //!< Number of variables marked as tabu required to trigger a reset.
		int restart_threshold; //!< Trigger a resart every 'restart_threshold' reset.
		int percent_to_reset; //<! Percentage of variables to randomly change the value at each reset.
		int number_start_samplings; //!< Number of variable assignments the solver randomly draw, if custom_starting_point and resume_search are false.
		
		Options();
		~Options() = default;

		Options( const Options& other );
		Options( Options&& other );
		
		Options& operator=( Options& other );
	};
}
