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


#include <numeric>
#include <iostream>
#include <algorithm>

#include "obj_rts.hpp"

using namespace std;

double regulation( const double x )
{
	return x >= 0 ? x : -(x*x)-1;
}

BestComposition::BestComposition( const vector<Variable>& variables,
                                  const vector< double >& coeff,
                                  const vector<vector<int>>& samples,
                                  std::function<double(double)> phi )
	: Maximize( variables, "Best composition" ),
	  _coeff(coeff),
	  _samples(samples),
	  phi(phi)
{ }

double BestComposition::required_cost( const vector<Variable*>& variables ) const
{
	vector<double> sols;

	int N = (int)_samples.size();

	// vecVariables:
	// H vs H, L vs H, R vs H
	// H vs L, L vs L, R vs L
	// H vs R, L vs R, R vs R	
	// + 'to produce' variables, but we don't care here
	
	// Coefficients:
	// H vs H, L vs H, R vs H
	// H vs L, L vs L, R vs L
	// H vs R, L vs R, R vs R	

	for( int i = 0 ; i < N ; ++i )
	{
		// min( 1, number ) to forbid overkill, ie, thinking for instance we can defeat 10 lights when the opponent can just have 3 of them (while having other kinds of unit)
		double tmp = regulation( std::min( 1.0, _coeff[0] * variables[0]->get_value() + _coeff[1] * variables[1]->get_value() + _coeff[2] * variables[2]->get_value() - _samples[i][0] ) ) //vs heavy
			+ regulation( std::min( 1.0, _coeff[3] * variables[3]->get_value() + _coeff[4] * variables[4]->get_value() + _coeff[5] * variables[5]->get_value() - _samples[i][1] ) ) //vs light
			+ regulation( std::min( 1.0, _coeff[6] * variables[6]->get_value() + _coeff[7] * variables[7]->get_value() + _coeff[8] * variables[8]->get_value() - _samples[i][2] ) ); //vs ranged

		sols.push_back(tmp);
	}

	std::sort( sols.begin(), sols.end() );

	double RDU = sols[0];

	for( int i = 1 ; i < sols.size() ; ++i )
		RDU += ( sols[i] - sols[i-1] ) * phi( static_cast<double>( N - i ) / N );

	return RDU;
}
