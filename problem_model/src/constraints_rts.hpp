#pragma once

#include <vector>

#include "ghost/constraint.hpp"
#include "ghost/variable.hpp"

using namespace std;
using namespace ghost;

class Stock : public Constraint
{
	int _heavyCost, _lightCost, _rangeCost;
	int _nb_barracks;
	double _stock;

	double required_cost() const override;

public:
	Stock( const vector< reference_wrapper<Variable> >& variables,
	       int heavyCost,
	       int lightCost,
	       int rangeCost,
	       int nb_barracks,
	       double stock );
};

class Assignment : public Constraint
{
	double _possessed_units;

	double required_cost() const override;

public:
	Assignment( const vector< reference_wrapper<Variable> >& variables,
	            double rhs );
};
