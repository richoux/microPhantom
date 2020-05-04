#include <iostream>
#include <algorithm>
#include <exception>

#include "constraints_rts.hpp"

using namespace std;
using namespace ghost;

/*******************
 ** Stock
 *******************/

Stock::Stock( const vector<reference_wrapper< Variable> >& variables,
              double stock )
	: Constraint(variables),
	  _stock(stock)
{ }

// 3*to_produce_H + 2*( to_produce_L + to_produce_R ) <= stock
double Stock::required_cost() const
{
	double sum = 3 * variables[0].get().get_value()
		+ 2 * ( variables[1].get().get_value() + variables[2].get().get_value() );

	return std::max( 0., sum - _stock );
}


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
