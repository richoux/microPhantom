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
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.NumberFormatException;

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
import rts.UnitActionAssignment;
import rts.UnitAction;

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


/**
 * @author Florian Richoux
 * (based upon POAdaptive by Valentin Antuari)
 */
public class MicroPhantom extends AbstractionLayerAI
{
	Random r = new Random();
	protected UnitTypeTable utt;

	public static boolean INFO = false;
	public static boolean DEBUG = false;

	public static int NB_SAMPLES = 50;

	public static PrintWriter writer_log;

	String solver_path;
	String solver_name;
	double[][] heat_map;

	int observed_worker = 0;
	int observed_light = 0;
	int observed_heavy = 0;
	int observed_ranged = 0;

	boolean random_version = false;
	boolean scout = false;
	long scout_ID = -1;

	int my_resource_patches;
	int number_melee_units;
	List<Unit> workers;

	int nb_samples;
	HashMap<Integer, HashMap> distribution_b;
	HashMap<Integer, HashMap> distribution_woutb;
	String distribution_file_b;
	String distribution_file_woutb;
	UnitType worker_type;
	UnitType base_type;
	UnitType barracks_type;
	UnitType light_type;
	UnitType heavy_type;
	UnitType ranged_type;

	boolean barracks;

	/*
	 * Constructors
	 */
	public MicroPhantom( UnitTypeTable a_utt,
	                     PathFinding a_pf,
	                     String distribution_file_b,
	                     String distribution_file_wb,
	                     String solver_path,
	                     double[][] heat_map )
	{
		this( a_utt, new AStarPathFinding(), distribution_file_b, distribution_file_wb, solver_path );
		if( heat_map != null )
		{
			this.heat_map = new double[ heat_map.length ][];
			for( int i = 0 ; i < heat_map.length ; ++i )
				this.heat_map[i] = heat_map[i].clone();
		}
	}

	public MicroPhantom( UnitTypeTable a_utt,
	                     String distribution_file_b,
	                     String distribution_file_wb,
	                     String solver_path )
	{
		this( a_utt, new AStarPathFinding(), distribution_file_b, distribution_file_wb, solver_path );
	}

	public MicroPhantom( UnitTypeTable a_utt,
	                     PathFinding a_pf,
	                     String distribution_file_b,
	                     String distribution_file_wb,
	                     String solver_path )
	{
		super( a_pf );
		reset( a_utt );
		this.solver_path = solver_path;
		this.nb_samples = NB_SAMPLES;
		this.distribution_file_b = distribution_file_b;
		this.distribution_file_woutb = distribution_file_wb;

		SAXBuilder sxb = new SAXBuilder();
		org.jdom.Document document;
		try
		{
			document = sxb.build( new File( distribution_file_b ) );
		}
		catch( Exception e )
		{
			document = null;
		}

		distribution_b = new HashMap<Integer, HashMap>();

		Element root = document.getRootElement();
		List<Element> time = root.getChildren( "time" );
		for( Element item : time )
		{
			String w [] = item.getChild("worker").getText().split(" ");
			String r [] = item.getChild("ranged").getText().split(" ");
			String h [] = item.getChild("heavy").getText().split(" ");
			String l [] = item.getChild("light").getText().split(" ");

			HashMap<String, List> tmp = new HashMap<String, List>();
			ArrayList<Float> listW = new ArrayList<Float>();
			ArrayList<Float> listR = new ArrayList<Float>();
			ArrayList<Float> listL = new ArrayList<Float>();
			ArrayList<Float> listH = new ArrayList<Float>();

			int end = w.length;
			for( int i = 0 ; i < end ; ++i )
				listW.add( Float.parseFloat( w[i] ) );

			end = r.length;
			for( int i = 0 ; i < end ; ++i )
				listR.add( Float.parseFloat( r[i] ) );

			end = l.length;
			for( int i = 0 ; i < end ; ++i )
				listL.add( Float.parseFloat( l[i] ) );

			end = h.length;
			for( int i = 0 ; i < end ; ++i )
				listH.add( Float.parseFloat( h[i] ) );

			tmp.put( "worker", listW );
			tmp.put( "ranged", listR );
			tmp.put( "light", listL );
			tmp.put( "heavy", listH );

			distribution_b.put( Integer.parseInt( item.getAttribute( "time" ).getValue() ), tmp );
		}

		try
		{
			writer_log = new PrintWriter( "src/ai/microPhantom/solver.log", "UTF-8" );
		}
		catch( IOException e1 )
		{
			System.out.println( "Exception with writer log" );
		}

		try
		{
			document = sxb.build( new File( distribution_file_woutb ) );
		}
		catch( Exception e )
		{
			document = null;
		}

		distribution_woutb = new HashMap<Integer, HashMap>();

		root = document.getRootElement();
		time = root.getChildren( "time" );
		for( Element item : time )
		{
			String w [] = item.getChild( "worker" ).getText().split(" ");
			String r [] = item.getChild( "ranged" ).getText().split(" ");
			String h [] = item.getChild( "heavy" ).getText().split(" ");
			String l [] = item.getChild( "light" ).getText().split(" ");

			HashMap<String, List> tmp = new HashMap<String, List>();
			ArrayList<Float> listW = new ArrayList<Float>();
			ArrayList<Float> listR = new ArrayList<Float>();
			ArrayList<Float> listL = new ArrayList<Float>();
			ArrayList<Float> listH = new ArrayList<Float>();

			int end = w.length;
			for( int i = 0 ; i < end ; ++i )
				listW.add( Float.parseFloat( w[i] ) );

			end = r.length;
			for( int i = 0 ; i < end ; ++i )
				listR.add( Float.parseFloat( r[i] ) );

			end = l.length;
			for( int i = 0 ; i < end ; ++i )
				listL.add( Float.parseFloat( l[i] ) );

			end = h.length;
			for( int i = 0 ; i < end ; ++i )
				listH.add( Float.parseFloat( h[i] ) );

			tmp.put( "worker", listW );
			tmp.put( "ranged", listR );
			tmp.put( "light", listL );
			tmp.put( "heavy", listH );

			distribution_woutb.put( Integer.parseInt( item.getAttribute( "time" ).getValue() ), tmp );
		}
	}

	/*
	 * Private methods
	 */
	private void initHeatMap( PhysicalGameState pgs, GameState gs )
	{
		int map_width = pgs.getWidth();
		int map_height = pgs.getHeight();
		heat_map = new double[ map_width ][ map_height ];
		if( gs instanceof PartiallyObservableGameState )
		{
			PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
			for( int i = 0 ; i < map_width ; ++i )
				for( int j = 0 ; j < map_height ; ++j )
					if( pogs.observable( i, j ) )
						heat_map[i][j] = gs.getTime();
					else
						heat_map[i][j] = -1.0;
		}
	}

	private void updateHeatMap( PhysicalGameState pgs, GameState gs )
	{
		int map_width = pgs.getWidth();
		int map_height = pgs.getHeight();
		if( gs instanceof PartiallyObservableGameState )
		{
			PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
			for( int i = 0 ; i < map_width ; ++i )
				for( int j = 0 ; j < map_height ; ++j )
					if( pogs.observable( i, j ) )
						heat_map[i][j] = gs.getTime();
		}
	}

	private double euclidianDistance( Unit u1, Unit u2 )
	{
		return Math.sqrt( Math.pow( u2.getX() - u1.getX(), 2 ) + Math.pow( u2.getY() - u1.getY(), 2 ) );
	}
	
	private int manhattanDistance( Unit u1, Unit u2 )
	{
		return Math.abs( u2.getX() - u1.getX() ) + Math.abs( u2.getY() - u1.getY() );
	}

	private int getSample( List<Float> distribution, int bypass )
	{
		double rnd = Math.random();
		float sum = 0.f;
		boolean start = false;
		int i = 0;

		while( i < bypass )
		{
			sum += distribution.get( i );
			++i;
		}

		rnd *= ( 1.0 - sum );
		sum =0.f;

		while( sum < rnd )
		{
			sum += distribution.get( i );
			++i;
		}

		return i - 1;
	}

	private boolean notOnBorder( int x, int y, PhysicalGameState pgs )
	{
		return x + 1 <= pgs.getWidth() && x - 1 >= 0 && y + 1 <= pgs.getHeight() && y - 1 >= 0;
	}
	
	private boolean freeAround( int x, int y, GameState gs, PhysicalGameState pgs )
	{
		return notOnBorder( x, y, pgs )
			&& gs.free( x    , y     )
			&& gs.free( x + 1, y     )
			&& gs.free( x - 1, y     )
			&& gs.free( x    , y + 1 )
			&& gs.free( x    , y - 1 );
	}

	/*
	 * 21 22 23 24 25 ... 
	 * 20  7  8  9 10
	 * 19  6  1  2 11
	 * 18  5  4  3 12
	 * 17 16 15 14 13
	 */		
	private void spiralSearch( AtomicInteger iX, AtomicInteger iY, GameState gs )
	{
		int x = iX.get();
		int y = iY.get();

		PhysicalGameState pgs = gs.getPhysicalGameState();
		int step = 0;
		int target = 1;
		boolean x_turn = true;
		while( !freeAround( x, y, gs, pgs ) )
		{
			++step;
			if( x_turn )
			{
				if( target %2 == 0 )
					--x;
				else
					++x;
				if( step == target )
				{
					step = 0;
					x_turn = false;
				}
			}
			else
			{
				if( target %2 == 0 )
					--y;
				else
					++y;
				if( step == target )
				{
					step = 0;
					x_turn = true;
					++target;
				}
			}
		}

		iX.set( x );
		iY.set( y );
	}

	/*
	 * Public methods
	 */
	@Override
	public void gameOver(int winner) throws Exception
	{
		System.out.println("Closing microPhantom AI");
		writer_log.close();
	}

	public AI clone()
	{
		return new MicroPhantom( utt, pf, distribution_file_b, distribution_file_woutb, solver_path, heat_map );
	}

	public void reset()
	{
		observed_worker = 0;
		observed_light = 0;
		observed_heavy = 0;
		observed_ranged = 0;
		heat_map = null;
		worker_type = utt.getUnitType( "Worker" );
		base_type = utt.getUnitType( "Base" );
		barracks_type = utt.getUnitType( "Barracks" );
		light_type = utt.getUnitType( "Light" );
		heavy_type = utt.getUnitType( "Heavy" );
		ranged_type = utt.getUnitType( "Ranged" );
		barracks = false;
		super.reset();
	}
	
	public void reset( UnitTypeTable a_utt )
	{
		utt = a_utt;
		this.reset();
	}

/*
  This is the main function of the AI. It is called at each game cycle with the most up to date game state and
  returns which actions the AI wants to execute in this cycle.
  The input parameters are:
  - player: the player that the AI controls (0 or 1)
  - gs: the current game state
  This method returns the actions to be sent to each of the units in the gamestate controlled by the player,
  packaged as a PlayerAction.
*/
	public PlayerAction getAction( int player, GameState gs )
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer( player );

		if( heat_map == null )
			initHeatMap( pgs, gs );
		else
			updateHeatMap( pgs, gs );

		long map_tiles = pgs.getWidth() * pgs.getHeight();
		double distance_threshold = Math.sqrt( map_tiles ) / 4;

		// determine how many resource patches I have near my bases, given a distance threshold
		my_resource_patches = 0;
		number_melee_units = 0;
		workers = new LinkedList<Unit>();
		AtomicInteger reserved_resources = new AtomicInteger( 0 );

		
		for( Unit u : pgs.getUnits() )
		{
			if( u.getType().isResource )
			{
				for( Unit b : pgs.getUnits() )
					if( b.getType().isStockpile && b.getPlayer() == player )
						if( euclidianDistance( u, b ) <= distance_threshold )
							++my_resource_patches;
			}
			else
				if( u.getPlayer() == player )
				{
					if( u.getType().canHarvest )
						workers.add( u );
					else
						if( u.getType().canAttack && gs.getActionAssignment(u) == null )
							++number_melee_units;
				}
		}

		for( Unit u : pgs.getUnits() )
		{
			if( u.getPlayer() == player && gs.getActionAssignment(u) == null )
			{
				// behavior of bases:
				if( u.getType().ID == base_type.ID )
					baseBehavior( u, p, pgs, workers.size(), reserved_resources );
				else				
					// behavior of barracks:
					if( u.getType().ID == barracks_type.ID )
						barracksBehavior( u, p, gs, pgs, gs.getTime(), reserved_resources );
					else
						// behavior of melee units:
						if( u.getType().canAttack && !u.getType().canHarvest )
						{
							// BASIC BEHAVIOR
							meleeUnitBehavior_heatmap( u, p, gs );
							
							// not BASIC BEHAVIOR
							// if( number_melee_units >= 4 )
							//	 meleeUnitBehavior_heatmap(u, p, gs);
							// else
							//	 meleeUnitBehavior(u, p, gs);
						}
			}
		}

		// behavior of workers:
		workersBehavior( workers, p, gs, reserved_resources );

		// This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
		return translateActions( player, gs );
	}

	@Override
	public List<ParameterSpecification> getParameters()
	{
		List<ParameterSpecification> parameters = new ArrayList<>();

		parameters.add( new ParameterSpecification( "PathFinding", PathFinding.class, new AStarPathFinding() ) );

		return parameters;
	}

	/*
	 * Protected methods
	 */
	protected void baseBehavior( Unit u, Player p, PhysicalGameState pgs, int nb_workers, AtomicInteger reserved_resources )
	{
		if( scout_ID != -1 )
			--nb_workers;

		// BASIC BEHAVIOR
		// if( nb_workers < 1 && p.getResources() >= worker_type.cost )
		// 	train( u, worker_type );

		// not BASIC BEHAVIOR
		// train 1 worker for each resource patch, excluding the scout
		if( nb_workers < my_resource_patches && p.getResources() >= worker_type.cost )
		{
			train( u, worker_type );
			reserved_resources.addAndGet( worker_type.cost );
		}

		// not BASIC BEHAVIOR
		// else if( !scout )
		// {
		//		 train(u, worker_type);
		//		 scout = true;
		// }
	}

	protected void meleeUnitCommonBehavior( Unit u, Player p, GameState gs, PhysicalGameState pgs, Unit closest_enemy, int closest_distance )
	{
		if( DEBUG )
		{
			UnitAction currentAction = gs.getUnitAction( u );
			System.out.println( "Action: " + currentAction );
		}

		if( u.getType().ID == ranged_type.ID && closest_distance <= 2 )
		{
			// In case we do not run away
			UnitActionAssignment ua = gs.getUnitActions().get( closest_enemy );

			if( closest_enemy.getType().ID == ranged_type.ID || ua == null || ( ua.action.getType() != UnitAction.TYPE_MOVE && closest_distance > 1 ) )
				attack( u, closest_enemy );
			else
			{
				// run away
				// we compute for each possible position the danger level
				int danger_up = 0;
				int danger_right = 0;
				int danger_down = 0;
				int danger_left = 0;

				// for each enemy unit
				for( Unit eu : pgs.getUnits() )
				{
					if( eu.getPlayer() >= 0 && eu.getPlayer() != p.getID() && eu.getType().canAttack )
					{
						//left
						if( eu.getX() == u.getX() - 2 && eu.getY() == u.getY() )
							++danger_right;

						//top-left
						if( eu.getX() == u.getX() - 1 && eu.getY() == u.getY() - 1 )
						{
							++danger_right;
							++danger_up;
						}

						//top
						if( eu.getX() == u.getX() && eu.getY() == u.getY() - 2 )
							++danger_up;

						//top-right
						if( eu.getX() == u.getX() + 1 && eu.getY() == u.getY() - 1 )
						{
							++danger_up;
							++danger_right;
						}

						//right
						if( eu.getX() == u.getX() + 2 && eu.getY() == u.getY() )
							++danger_right;

						//bottom-right
						if( eu.getX() == u.getX() + 1 && eu.getY() == u.getY() + 1 )
						{
							++danger_right;
							++danger_down;
						}

						//bottom
						if( eu.getX() == u.getX() && eu.getY() == u.getY() + 2 )
							++danger_down;

						//bottom-left
						if( eu.getX() == u.getX() - 1 && eu.getY() == u.getY() + 1 )
						{
							++danger_down;
							++danger_left;
						}
					}
				}

				// up
				if( u.getY() - 1 <= 0 || !gs.free( u.getX(), u.getY() - 1 ) )
					danger_up = 10000;

				// right
				if( u.getX() + 1 >= pgs.getWidth() || !gs.free( u.getX() + 1, u.getY() ) ) 
					danger_right = 10000;

				// down
				if( u.getY() + 1 >= pgs.getHeight() || !gs.free( u.getX(), u.getY() + 1 ) )
					danger_down = 10000;

				// left
				if( u.getX() - 1 <= 0 || !gs.free( u.getX() - 1, u.getY() ) )
					danger_left = 10000;

				// We take the safer position
				if( danger_up <= danger_left )
				{
					if( danger_up <= danger_down )
					{
						if( danger_up <= danger_right )
						{
							// move up
							move( u, u.getX(), u.getY() - 1 );
						}
						else
						{
							// move right
							move( u, u.getX() + 1, u.getY() );
						}
					}
					else
					{
						if( danger_down <= danger_right )
						{
							// move down
							move( u, u.getX(), u.getY() + 1 );
						}
						else
						{
							// move right
							move( u, u.getX() + 1, u.getY() );
						}
					}
				}
				else
				{
					if( danger_left <= danger_down )
					{
						if( danger_left <= danger_right )
						{
							// move left
							move( u, u.getX() - 1, u.getY() );
						}
						else
						{
							// move right
							move( u, u.getX() + 1, u.getY() );
						}
					}
					else
					{
						if( danger_down <= danger_right )
						{
							// move down
							move( u, u.getX(), u.getY() + 1 );
						}
						else
						{
							// move right
							move( u, u.getX() + 1, u.getY() );
						}
					}
				}

				// if( DEBUG )
				// {
				// 	System.out.println( "Position : " + u.getX() + ", " + u.getY() );
				// 	System.out.println( "Danger: URDL : " + danger_up + " " + danger_right + " " + danger_down + " " + danger_left );
				// 	if( danger_up <= danger_left )
				// 	{
				// 		if( danger_up <= danger_down )
				// 		{
				// 			if( danger_up <= danger_right )
				// 			{
				// 				// move up
				// 				System.out.println( "Move to : " + u.getX() + " " + ( u.getY() - 1 ) );
				// 			}
				// 			else
				// 			{
				// 				// move right
				// 				System.out.println( "Move to : " + ( u.getX() + 1 ) + " " + u.getY() );
				// 			}
				// 		}
				// 		else
				// 		{
				// 			if( danger_down <= danger_right )
				// 			{
				// 				// move down
				// 				System.out.println( "Move to : " + u.getX() + " " + ( u.getY() + 1 ) );
				// 			}
				// 			else
				// 			{
				// 				// move right
				// 				System.out.println( "Move to : " +( u.getX() + 1 ) + " " + u.getY() ); 
				// 			}
				// 		}
				// 	}
				// 	else
				// 	{
				// 		if( danger_left <= danger_down )
				// 		{
				// 			if( danger_left <= danger_right )
				// 			{
				// 				// move left
				// 				System.out.println( "Move to : " + ( u.getX() - 1 ) + " " + u.getY() );
				// 			}
				// 			else
				// 			{
				// 				// move right
				// 				System.out.println( "Move to : " + ( u.getX() + 1 ) + " " + u.getY() );
				// 			}
				// 		}
				// 		else
				// 		{
				// 			if( danger_down <= danger_right )
				// 			{
				// 				// move down
				// 				System.out.println( "Move to : " + u.getX() + " " + ( u.getY() + 1 ) );
				// 			}
				// 			else
				// 			{
				// 				// move right
				// 				System.out.println( "Move to : " + ( u.getX() + 1 ) + " " + u.getY() ); 
				// 			}
				// 		}
				// 	}

				// 	try
				// 	{
				// 		System.in.read();
				// 	}
				// 	catch( Exception e )
				// 	{
				// 		//TODO: handle exception
				// 	}
				// }
			}
		} 
		else //ie, if not ( u.getType().ID == ranged_type.ID && closest_distance <= 2 )
		{
			UnitAction current_action = gs.getUnitAction( u );
			boolean attack_closest_enemy = true;

			if( current_action != null && current_action.getType() == UnitAction.TYPE_ATTACK_LOCATION )
			{
				int x = current_action.getLocationX();
				int y = current_action.getLocationY();
				int d = Math.abs( x - u.getX() ) + Math.abs( y - u.getY() );

				if( d <= 2 )
				{
					Unit enemy = pgs.getUnitAt( x, y );
					if( enemy != null )
					{
						attack( u, enemy );
						attack_closest_enemy = false;
					}
				}
			}

			if( attack_closest_enemy )
				attack( u, closest_enemy );
		}
	}

	protected void meleeUnitBehavior_heatmap( Unit u, Player p, GameState gs )
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Unit closest_enemy = null;
		int closest_distance = Integer.MAX_VALUE;

		for( Unit eu : pgs.getUnits() )
			if( eu.getPlayer() >= 0 && eu.getPlayer() != p.getID() )
			{
				int d = manhattanDistance( u, eu );
				if( d < closest_distance )
				{
					closest_enemy = eu;
					closest_distance = d;
				}
			}

		if( closest_enemy != null )
			meleeUnitCommonBehavior( u, p, gs, pgs, closest_enemy, closest_distance );
		else
			if( gs instanceof PartiallyObservableGameState )
			{
				PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
				// there are no enemies, so we need to explore (find the nearest non-observable place):
				int min_x = 0;
				int min_y = 0;
				// closest_distance = -1;
				double heat_point = -10000.0;

				for( int i = 0 ; i < pgs.getWidth() ; ++i )
					for( int j = 0 ; j < pgs.getHeight() ; ++j )
					{
						if( heat_map[i][j] < heat_point || heat_point <= -10000.0 )
						{
							heat_point = heat_map[i][j];
							min_x = i;
							min_y = j;
						}
						else
							if( heat_map[i][j] <= heat_point )
								if( Math.random() <= 0.5 )
								{
									heat_point = heat_map[i][j];
									min_x = i;
									min_y = j;
								}
					}

				if( heat_point > -10000.0 )
				{
					move( u, min_x, min_y );
					if( DEBUG )
						System.out.println("Unit " + u + " moves to (" + min_x + ", " + min_y + ")");
				}
				else
				{
					int rand_x = (int)(pgs.getWidth() * Math.random() );
					int rand_y = (int)(pgs.getHeight() * Math.random() );
					move( u, rand_x, rand_y );
					if( DEBUG )
						System.out.println("Unit " + u + " moves RANDOM to (" + rand_x + ", " + rand_y + ")");
				}

				if( DEBUG )
				{
					UnitAction current_action = gs.getUnitAction( u );
					System.out.println("Action: " + current_action );
				}
			}
	}

	protected void meleeUnitBehavior( Unit u, Player p, GameState gs )
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Unit closest_enemy = null;
		int closest_distance = Integer.MAX_VALUE;

		for( Unit eu : pgs.getUnits() )
			if( eu.getPlayer() >= 0 && eu.getPlayer() != p.getID() )
			{
				int d = manhattanDistance( u, eu );
				if( d < closest_distance )
				{
					closest_enemy = eu;
					closest_distance = d;
				}
			}

		if( closest_enemy != null )
			meleeUnitCommonBehavior( u, p, gs, pgs, closest_enemy, closest_distance );
		else
		{
			if( gs instanceof PartiallyObservableGameState )
			{
				PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
				// there are no enemies, so we need to explore (find the nearest non-observable place):
				int closest_x = 0;
				int closest_y = 0;
				closest_distance = -1;

				for( int i = 0 ; i < pgs.getHeight() ; ++i )
					for( int j = 0 ; j < pgs.getWidth() ; ++j )
						if( !pogs.observable( j, i ) )
						{
							int d = ( u.getX() - j ) * ( u.getX() - j ) + ( u.getY() - i ) * ( u.getY() - i );
							if( closest_distance == -1 || d < closest_distance )
							{
								closest_x = j;
								closest_y = i;
								closest_distance = d;
							}
						}

				if( closest_distance != -1 )
					move( u, closest_x, closest_y );
			}
		}
	}

	protected void barracksBehavior( Unit u, Player p, GameState gs, PhysicalGameState pgs, int time, AtomicInteger reserved_resources )
	{
		int enemyWorker = 0;
		int enemyRanged = 0;
		int enemyLight = 0;
		int enemyHeavy = 0;

		int playerHeavy = 0;
		int playerRanged = 0;
		int playerLight = 0;
		int playerWorker = 0;
		int playerIdleBarracks = 0;

		int sol_heavy = 0;
		int sol_ranged = 0;
		int sol_light = 0;

		time = time - ( time % 10 );
		if( p.getResources() >= 2 )
		{
			if( INFO )
				System.out.println( "Resources: " + p.getResources() );

			// counts units of each player per type
			for( Unit u2 : pgs.getUnits() )
			{
				if( u2.getPlayer() >= 0 && u2.getPlayer() != p.getID() )
				{
					if( u2.getType().ID == worker_type.ID )
						++enemyWorker;
					else if( u2.getType().ID == heavy_type.ID )
						++enemyHeavy;
					else if( u2.getType().ID == light_type.ID )
						++enemyLight;
					else if( u2.getType().ID == ranged_type.ID )
						++enemyRanged;
				}
				else
					if( u2.getPlayer() >= 0 && u2.getPlayer() == p.getID() )
					{
						if( u2.getType().ID == worker_type.ID )
							++playerWorker;
						else if( u2.getType().ID == heavy_type.ID )
							++playerHeavy;
						else if( u2.getType().ID == light_type.ID )
							++playerLight;
						else if( u2.getType().ID == ranged_type.ID )
							++playerRanged;
						else if( u2.getType().ID == barracks_type.ID )
						{
							UnitAction ua = gs.getUnitAction( u2 );
							if( ua == null || ua.getType() == UnitAction.TYPE_NONE )
								++playerIdleBarracks;
						}
					}
			}

			observed_worker =	Math.max( observed_worker, enemyWorker );
			observed_heavy =	 Math.max( observed_heavy, enemyHeavy );
			observed_ranged =	Math.max( observed_ranged, enemyRanged );
			observed_light =	 Math.max( observed_light, enemyLight );

			if( observed_heavy > 0 || observed_light > 0 || observed_ranged > 0 )
				barracks = true;

			if( INFO )
				System.out.println( "Barrack: " + barracks );

			// Draws
			ArrayList<Integer[]> samples = new ArrayList<Integer[]>();
			Double[] info = { 0.0, 0.0, 0.0, 0.0 };

			for( int i = 0 ; i <= nb_samples ; ++i )
			{
				Integer[] tmp = new Integer[4];
				if( barracks )
				{
					tmp[0] = 1;
					tmp[1] = getSample( (List)distribution_b.get( time ).get( "heavy" ), observed_heavy );
					tmp[2] = getSample( (List)distribution_b.get( time ).get( "ranged" ), observed_ranged );
					tmp[3] = getSample( (List)distribution_b.get( time ).get( "light" ), observed_light );
				}
				else
				{
					tmp[0] = getSample( (List)distribution_woutb.get( time ).get( "worker" ), observed_worker );
					tmp[1] = getSample( (List)distribution_woutb.get( time ).get( "heavy" ), observed_heavy );
					tmp[2] = getSample( (List)distribution_woutb.get( time ).get( "ranged" ), observed_ranged );
					tmp[3] = getSample( (List)distribution_woutb.get( time ).get( "light" ), observed_light );
				}

				samples.add( tmp );
				info[0] += tmp[0];
				info[1] += tmp[1];
				info[2] += tmp[2];
				info[3] += tmp[3];
			}

			if( INFO )
			{
				System.out.println( "Samples moy = W" + info[0] / nb_samples
				                    + " / H" + info[1] / nb_samples
				                    + " / R" + info[2] / nb_samples
				                    + " / L" + info[3] / nb_samples );
				System.out.println( " Units player(" + ( playerHeavy + playerRanged + playerLight )
				                    + "+" + playerWorker + ") : W" + playerWorker + " / H" + playerHeavy+ " / R" + playerRanged + " / L" + playerLight );
				System.out.println( " Units observed(" + ( observed_heavy + observed_ranged + observed_light )
				                    + "+" + observed_worker + ") : W" + observed_worker + " / H" + observed_heavy + " / R"
				                    + observed_ranged + " / L" + observed_light );
			}

			// write parameter for solver in a file
			try
			{
				PrintWriter writer = new PrintWriter( "src/ai/microPhantom/data_solver", "UTF-8" );
				writer_log.println( "Time: " + time );

				// Samples indexes:
				// 0 for worker
				// 1 for heavy
				// 2 for ranged
				// 3 for light
				for( int i = 0 ; i < nb_samples ; ++i )
				{
					writer.println( samples.get(i)[0] + " " + samples.get(i)[1] + " " + samples.get(i)[2] + " " + samples.get(i)[3] );
					writer_log.println( samples.get(i)[0] + " " + samples.get(i)[1] + " " + samples.get(i)[2] + " " + samples.get(i)[3] );
				}

				writer.println( heavy_type.cost );
				writer.println( ranged_type.cost );
				writer.println( light_type.cost );
				writer_log.println( heavy_type.cost );
				writer_log.println( ranged_type.cost );
				writer_log.println( light_type.cost );

				writer.println( playerIdleBarracks );
				writer_log.println( playerIdleBarracks );

				writer.println( playerHeavy );
				writer.println( playerRanged );
				writer.println( playerLight );
				writer_log.println( playerHeavy );
				writer_log.println( playerRanged );
				writer_log.println( playerLight );

				// if( p.getResources() <= 2 )
				// 	writer.println( 3 );
				// else
				// 	writer.println( p.getResources() );

				writer.println( p.getResources() );
				writer_log.println( p.getResources() );

				writer_log.println("#########\n");
				writer.close();
			}
			catch( IOException e1 )
			{
				System.out.println( "Exception in printer" );
			}

			// get solutions
			boolean no_train = false;
			try
			{
				// System.out.println("Hello Java");
				Runtime r = Runtime.getRuntime();

				if( pgs.getWidth() >= 20 )
					solver_name = solver_path + "solver_cpp_optimistic";
				else
					solver_name = solver_path + "solver_cpp_pessimistic";

				Process process = r.exec( String.format( "%s %s %d", solver_name, "src/ai/microPhantom/data_solver", nb_samples ) );
				process.waitFor();

				BufferedReader b = new BufferedReader( new InputStreamReader( process.getInputStream() ) );

				if( INFO )
				{
					System.out.println( "Trace after calling the solver:" );
					System.out.println( b.readLine() );
					System.out.println( b.readLine() );
					System.out.println( b.readLine() );
				}

				sol_heavy = Integer.parseInt( b.readLine() );
				sol_ranged = Integer.parseInt( b.readLine() );
				sol_light = Integer.parseInt( b.readLine() );

				System.out.println( "H" + sol_heavy + " R" + sol_ranged + " L" + sol_light + "\n" );

				if( INFO )
					System.out.println( "H" + sol_heavy + " R" + sol_ranged + " L" + sol_light + "\n\n" );
				b.close();
			}
			catch( IOException e1 )
			{
				System.out.println( "IO exception in process" );
				System.out.println( e1.getMessage() );
			}
			catch( InterruptedException e2 )
			{
				System.out.println( "interupt exception in process" );
			}
			catch( NumberFormatException e3 )
			{
				no_train = true;
				System.out.println( "No train" );
			}

			if( !no_train )
				if( sol_light >= sol_ranged )
				{
					if( sol_light >= sol_heavy )
					{
						train( u, light_type );
						reserved_resources.addAndGet( light_type.cost );
					}
					else
						if( p.getResources() >= heavy_type.cost )
						{
							train( u, heavy_type );
							reserved_resources.addAndGet( heavy_type.cost );
						}
				}
				else
					if( sol_ranged >= sol_heavy )
					{
						train( u, ranged_type );
						reserved_resources.addAndGet( ranged_type.cost );
					}
					else
						if( p.getResources() >= heavy_type.cost )
						{
							train( u, heavy_type );
							reserved_resources.addAndGet( heavy_type.cost );							
						}
		}
	}

	protected void workersBehavior( List<Unit> workers, Player p, GameState gs, AtomicInteger reserved_resources )
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		int nb_bases = 0;
		int nb_barracks = 0;

		List<Unit> free_workers = new LinkedList<Unit>();

		// BASIC BEHAVIOR
		free_workers.addAll( workers );

		// not BASIC BEHAVIOR
		// for( Unit w : workers )
		//	 if( w.getID() != scout_ID )
		//		 free_workers.add( w );
		//	 else
		//			 meleeUnitBehavior_heatmap(w, p, gs);

		// // if our scout died
		// if( scout && scout_ID != -1 && workers.size() == free_workers.size() )
		//	 scout_ID = -1;

		// if( scout && scout_ID == -1 && !free_workers.isEmpty() && free_workers.get(0) != null )
		// {
		//	 Unit w = free_workers.remove(0);
		//	 scout_ID = w.getID();
		//	 meleeUnitBehavior_heatmap(w, p, gs);
		// }

		if( workers.isEmpty() )
			return;

		for( Unit u2 : pgs.getUnits() )
		{
			if( u2.getType().ID == base_type.ID && u2.getPlayer() == p.getID() )
				nb_bases++;

			if( u2.getType().ID == barracks_type.ID && u2.getPlayer() == p.getID() )
				nb_barracks++;
		}

		List<Integer> reserved_positions = new LinkedList<Integer>();
		if( nb_bases == 0 && !free_workers.isEmpty() )
		{
			// build a base:
			if( p.getResources() >= base_type.cost + reserved_resources.get() )
			{
				Unit u = free_workers.remove( 0 );
				AtomicInteger new_building_x = new AtomicInteger( u.getX() );
				AtomicInteger new_building_y = new AtomicInteger( u.getY() );
				spiralSearch( new_building_x, new_building_y, gs );
				buildIfNotAlreadyBuilding( u, base_type, new_building_x.get(), new_building_y.get(), reserved_positions, p, pgs );
				reserved_resources.addAndGet( base_type.cost );
			}
		}

		if( nb_barracks == 0 )
		{
			// build a barracks:
			if( p.getResources() >= barracks_type.cost + reserved_resources.get() && !free_workers.isEmpty() )
			{
				Unit u = free_workers.remove( 0 );
				AtomicInteger new_building_x = new AtomicInteger( u.getX() );
				AtomicInteger new_building_y = new AtomicInteger( u.getY() );
				spiralSearch( new_building_x, new_building_y, gs );
				buildIfNotAlreadyBuilding( u, barracks_type, new_building_x.get(), new_building_y.get(), reserved_positions, p, pgs );
				reserved_resources.addAndGet( barracks_type.cost );
			}
		}

		// harvest with all the free workers:
		for( Unit u : free_workers )
		{
			Unit closest_base = null;
			Unit closest_resource = null;
			int closest_distance = Integer.MAX_VALUE;

			for( Unit u2 : pgs.getUnits() )
				if( u2.getType().isResource )
				{
					int d = manhattanDistance( u, u2 );
					if( d < closest_distance )
					{
						closest_resource = u2;
						closest_distance = d;
					}
				}

			closest_distance = Integer.MAX_VALUE;
			for( Unit u2 : pgs.getUnits() )
				if( u2.getType().isStockpile && u2.getPlayer() == p.getID() )
				{
					int d = manhattanDistance( u, u2 );
					if( d < closest_distance )
					{
						closest_base = u2;
						closest_distance = d;
					}
				}

			if( closest_resource != null && closest_base != null )
			{
				AbstractAction aa = getAbstractAction( u );
				if( aa instanceof Harvest )
				{
					Harvest h_aa = (Harvest)aa;
					if( h_aa.getTarget() != closest_resource || h_aa.getBase() != closest_base )
						harvest( u, closest_resource, closest_base );
				}
				else
					harvest( u, closest_resource, closest_base );
			}
			// not BASIC behavior
			// explore if no resource around. Remember where were far resources.
		}
	}
}
