#pragma once

#include <vector>

#include "ghost/constraint.hpp"
#include "ghost/variable.hpp"

using namespace std;
using namespace ghost;

class Stock : public Constraint
{
	double _stock;

	double required_cost() const override;

public:
	Stock( const vector< reference_wrapper<Variable> >& variables,
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
