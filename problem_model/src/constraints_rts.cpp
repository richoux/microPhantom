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
              int heavyCost,
              int lightCost,
              int rangeCost,
              int nb_barracks,
              double stock )
	: Constraint(variables),
	  _heavyCost(heavyCost),
	  _lightCost(lightCost),
	  _rangeCost(rangeCost),
	  _nb_barracks(nb_barracks),
	  _stock(stock)
{ }

// H_cost*to_produce_H + L_cost*to_produce_L + R_cost*to_produce_R <= stock
double Stock::required_cost() const
{
	double sum = _heavyCost * variables[0].get().get_value()
	           + _lightCost * variables[1].get().get_value()
	           + _rangeCost * variables[2].get().get_value();

	return std::max( 0., sum - ( _stock + _nb_barracks ) );
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
