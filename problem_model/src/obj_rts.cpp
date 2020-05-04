#include <numeric>
#include <iostream>

#include "obj_rts.hpp"

using namespace std;

double regulation( const double x )
{
	return x >= 0 ? x : -(1+x)*(1+x);
}

BestComposition::BestComposition( const vector< double >& coeff,
                                  const vector<vector<int>>& samples,
                                  std::function<double(double)> phi )
	: Objective( "Best composition" ),
	  _coeff(coeff),
	  _samples(samples),
	  phi(phi)
{ }

double BestComposition::required_cost( const vector< Variable >& vecVariables ) const
{
	vector<double> sols;

	int N = (int)_samples.size();

	// Samples indexes:
	// 0 for worker
	// 1 for heavy
	// 2 for ranged
	// 3 for light

	// vecVariables:
	// L vs H, H vs H, R vs H
	// L vs L, H vs L, R vs L
	// L vs R, H vs R, R vs R	
	// + 'to produce' variables, but we don't care here
	
	// Coefficients:
	// L vs H, H vs H, R vs H
	// L vs L, H vs L, R vs L
	// L vs R, H vs R, R vs R	

	for( int i = 0 ; i < N ; ++i )
	{
		double tmp = regulation( _coeff[0] * vecVariables[0].get_value() + _coeff[1] * vecVariables[1].get_value() + _coeff[2] * vecVariables[2].get_value() - _samples[i][1] ) //vs heavy
			+ regulation( _coeff[3] * vecVariables[3].get_value() + _coeff[4] * vecVariables[4].get_value() + _coeff[5] * vecVariables[5].get_value() - _samples[i][3] ) //vs light
			+ regulation( _coeff[6] * vecVariables[6].get_value() + _coeff[7] * vecVariables[7].get_value() + _coeff[8] * vecVariables[8].get_value() - _samples[i][2] ); //vs ranged

		sols.push_back(tmp);
	}

	std::sort( sols.begin(), sols.end() );

	double RDU = sols[0];

	for( int i = 1 ; i < sols.size() ; ++i )
		RDU += ( sols[i] - sols[i-1] ) * phi( static_cast<double>( N - i ) / N );

	return -RDU;
}
