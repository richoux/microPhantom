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


package ai.microPhantom;

import java.util.concurrent.ThreadLocalRandom;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.*;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.GameState;
import rts.PlayerAction;
import rts.units.*;

import org.jdom.*;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Random;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;


public class RandomMicroPhantom extends MicroPhantom {
	
	boolean heavy=false;
	
	public RandomMicroPhantom(UnitTypeTable a_utt, String distribution_file_b, String distribution_file_wb, String solver)
	{
		this(a_utt, new AStarPathFinding(), distribution_file_b, distribution_file_wb, solver);
	}

	public RandomMicroPhantom(UnitTypeTable a_utt, PathFinding a_pf, String distribution_file_b, String distribution_file_wb, String solver)
	{
		super(a_utt, a_pf, distribution_file_b, distribution_file_wb, solver);
	}

	@Override
	public AI clone()
	{
		return new RandomMicroPhantom(utt, pf, distribution_file_b, distribution_file_woutb, solver_name);
	}
	
	@Override
	protected void barracksBehavior(Unit u, Player p, PhysicalGameState pgs, int time)
	{
		if( heavy )
		{
			if( p.getResources() >= heavyType.cost )
			{
				train( u, heavyType );
				heavy = false;
			}
		}
		else
		{
			if( p.getResources() >= 2 )
			{
				int randomNum = ThreadLocalRandom.current().nextInt( 0, 3 );
                
				switch( randomNum )
				{
				case 0:
					if( p.getResources() >= heavyType.cost )
						train( u, heavyType );
					else
						heavy=true;
					break;
				case 1:
					if( p.getResources() >= rangedType.cost )
						train( u, rangedType );
					break;
				case 2:
					if( p.getResources() >= lightType.cost )
						train( u, lightType );
					break;
				}
			}
		}
	}
}
