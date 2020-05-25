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

#include <cmath>
#include <functional>
#include <algorithm>

std::function<double(double)> logistic( int lambda, double shift )
{
	return [lambda, shift](double p){ return 1.0 / ( 1 + exp( - lambda * (2*p - shift) ) ); };
}

std::function<double(double)> logit( int lambda )
{
	return [lambda](double p)
				 {
					 if( p < 0.005 ) return 0.0;
					 if( p > 0.995 ) return 1.0;
					 return std::max( 0., 1 + log( p / ( 2 - p ) ) / lambda ) ;
				 };
}

////////////////

std::function<double(double)> identity()
{
	return [](double p){ return p; };
}

std::function<double(double)> pessimistic()
{
	return logistic( 10, 1.3 );
}

std::function<double(double)> optimistic()
{
	return logit( 10 );
}
