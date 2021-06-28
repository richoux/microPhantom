#include <iostream>
#include <iomanip>
#include <string>
#include <cmath>

#include "print_test.hpp"

stringstream PrintTest::print_candidate( const std::vector<Variable>& variables ) const
{
	stringstream stream;

	stream << "Heavy assigned to heavy:   " << variables[0].get_value() << "\n";
	stream << "Light assigned to heavy:   " << variables[1].get_value() << "\n";
	stream << "Ranged assigned to heavy:  " << variables[2].get_value() << "\n";
	stream << "Heavy assigned to light:   " << variables[3].get_value() << "\n";
	stream << "Light assigned to light:   " << variables[4].get_value() << "\n";
	stream << "Ranged assigned to light:  " << variables[5].get_value() << "\n";
	stream << "Heavy assigned to ranged:  " << variables[6].get_value() << "\n";
	stream << "Light assigned to ranged:  " << variables[7].get_value() << "\n";
	stream << "Ranged assigned to ranged: " << variables[8].get_value() << "\n";
	stream << "-----------------------------\n";
	stream << "Heavy to produce:  " << variables[9].get_value() << "\n";
	stream << "Light to produce:  " << variables[10].get_value() << "\n";
	stream << "Ranged to produce: " << variables[11].get_value() << "\n";
		
	return stream;
}
