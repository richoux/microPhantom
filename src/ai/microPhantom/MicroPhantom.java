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

	public static int NB_SAMPLES = 50;

	public static PrintWriter writer_log;

	String solver_path;
	String solver_name;
	int[][] heat_map;

	int observed_worker = 0;
	int observed_light = 0;
	int observed_heavy = 0;
	int observed_ranged = 0;

	boolean random_version = false;
	boolean scout = false;
	long scout_ID = -1;

	int number_units_can_attack;

	List<Unit> resource_patches;
	List<Unit> my_resource_patches;
	
	List<Unit> my_units;
	List<Unit> my_bases;
	List<Unit> my_barracks;
	List<Unit> my_workers;
	List<Unit> my_army;
	List<Unit> my_melee_units;
	List<Unit> my_heavy_units;
	List<Unit> my_light_units;
	List<Unit> my_ranged_units;

	List<Unit> enemy_units;
	List<Unit> enemy_bases;
	List<Unit> enemy_barracks;
	List<Unit> enemy_workers;
	List<Unit> enemy_army;
	List<Unit> enemy_melee_units;
	List<Unit> enemy_heavy_units;
	List<Unit> enemy_light_units;
	List<Unit> enemy_ranged_units;

	int nb_samples;
	HashMap<Integer, HashMap> distribution_b;
	HashMap<Integer, HashMap> distribution_woutb;
	String distribution_file_b;
	String distribution_file_woutb;

	UnitType base_type;
	UnitType barracks_type;
	UnitType worker_type;
	UnitType heavy_type;
	UnitType light_type;
	UnitType ranged_type;

	boolean enemy_has_barracks;

	/*
	 * Constructors
	 */
	public MicroPhantom( UnitTypeTable a_utt,
	                     PathFinding a_pf,
	                     String distribution_file_b,
	                     String distribution_file_wb,
	                     String solver_path,
	                     int[][] heat_map )
	{
		this( a_utt, new AStarPathFinding(), distribution_file_b, distribution_file_wb, solver_path );
		if( heat_map != null )
		{
			this.heat_map = new int[ heat_map.length ][];
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
		heat_map = new int[ map_width ][ map_height ];
		if( gs instanceof PartiallyObservableGameState )
		{
			PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
			for( int i = 0 ; i < map_width ; ++i )
				for( int j = 0 ; j < map_height ; ++j )
					if( pogs.observable( i, j ) )
						heat_map[i][j] = gs.getTime();
					else
						heat_map[i][j] = -1;
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

	private void scanUnits(  PhysicalGameState pgs, GameState gs, Player player )
	{
		resource_patches.clear();
		
		my_units.clear();
		my_bases.clear();
		my_barracks.clear();
		my_workers.clear();
		my_army.clear();
		my_melee_units.clear();
		my_heavy_units.clear();
		my_light_units.clear();
		my_ranged_units.clear();

		enemy_units.clear();
		enemy_bases.clear();
		enemy_barracks.clear();
		enemy_workers.clear();
		enemy_army.clear();
		enemy_melee_units.clear();
		enemy_heavy_units.clear();
		enemy_light_units.clear();
		enemy_ranged_units.clear();

		for( Unit u : pgs.getUnits() )
		{
			if( u.getType().isResource )
				resource_patches.add( u );
			else
			{
				if( u.getPlayer() == player.getID() )
				{
					my_units.add( u );
					if( u.getType().ID == base_type.ID )
						my_bases.add( u );
					else if( u.getType().ID == barracks_type.ID )
						my_barracks.add( u );
					else if( u.getType().ID == worker_type.ID )
						my_workers.add( u );
					else if( u.getType().ID == heavy_type.ID )
					{
						my_heavy_units.add( u );
						my_army.add( u );
						my_melee_units.add( u );
					}
					else if( u.getType().ID == light_type.ID )
					{
						my_light_units.add( u );
						my_army.add( u );
						my_melee_units.add( u );
					}
					else if( u.getType().ID == ranged_type.ID )
					{
						my_ranged_units.add( u );
						my_army.add( u );
					}
				}
				else
					if( u.getPlayer() >= 0 && u.getPlayer() != player.getID() )
					{
						enemy_units.add( u );
						if( u.getType().ID == base_type.ID )
							enemy_bases.add( u );
						else if( u.getType().ID == barracks_type.ID )
							enemy_barracks.add( u );
						else if( u.getType().ID == worker_type.ID )
							enemy_workers.add( u );
						else if( u.getType().ID == heavy_type.ID )
						{
							enemy_heavy_units.add( u );
							enemy_army.add( u );
							enemy_melee_units.add( u );
						}
						else if( u.getType().ID == light_type.ID )
						{
							enemy_light_units.add( u );
							enemy_army.add( u );
							enemy_melee_units.add( u );
						}
						else if( u.getType().ID == ranged_type.ID )
						{
							enemy_ranged_units.add( u );
							enemy_army.add( u );
						}
					}
			}
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

	private Unit getClosestEnemy( Unit u, Player player, PhysicalGameState pgs )
	{
		Unit closest_enemy = null;
		int closest_distance = Integer.MAX_VALUE;
		
		for( Unit eu : enemy_units )
		{
			int d = manhattanDistance( u, eu );
			if( d < closest_distance )
			{
				closest_enemy = eu;
				closest_distance = d;
			}
		}

		return closest_enemy;
	}
	
	/*
	 * Public methods
	 */
	@Override
	public void gameOver( int winner ) throws Exception
	{
		System.out.println("Closing microPhantom");
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

		enemy_has_barracks = false;

		resource_patches = new ArrayList<Unit>();
		my_resource_patches = new ArrayList<Unit>();
		
		my_units = new ArrayList<Unit>();
		my_bases = new ArrayList<Unit>();
		my_barracks = new ArrayList<Unit>();
		my_workers = new ArrayList<Unit>();
		my_army = new ArrayList<Unit>();
		my_melee_units = new ArrayList<Unit>();
		my_heavy_units = new ArrayList<Unit>();
		my_light_units = new ArrayList<Unit>();
		my_ranged_units = new ArrayList<Unit>();

		enemy_units = new ArrayList<Unit>();
		enemy_bases = new ArrayList<Unit>();
		enemy_barracks = new ArrayList<Unit>();
		enemy_workers = new ArrayList<Unit>();
		enemy_army = new ArrayList<Unit>();
		enemy_melee_units = new ArrayList<Unit>();
		enemy_heavy_units = new ArrayList<Unit>();
		enemy_light_units = new ArrayList<Unit>();
		enemy_ranged_units = new ArrayList<Unit>();

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
	public PlayerAction getAction( int p, GameState gs )
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player player = gs.getPlayer( p );

		if( heat_map == null )
			initHeatMap( pgs, gs );
		else
			updateHeatMap( pgs, gs );

		scanUnits( pgs, gs, player );
		
		long map_tiles = pgs.getWidth() * pgs.getHeight();
		double distance_threshold = Math.sqrt( map_tiles ) / 4;

		// determine how many resource patches I have near my bases, given a distance threshold
		my_resource_patches.clear();
		number_units_can_attack = 0;
		AtomicInteger reserved_resources = new AtomicInteger( 0 );


		for( Unit u : resource_patches )
		{
			for( Unit b : my_bases )
				if( euclidianDistance( u, b ) <= distance_threshold )
				{
					my_resource_patches.add( u );
					break; // don't check it for another base
				}
		}

		for( Unit u : my_army )
			if( gs.getActionAssignment( u ) == null )
				++number_units_can_attack;

		for( Unit u : my_workers )
			if( gs.getActionAssignment( u ) == null )
				++number_units_can_attack;

		for( Unit u : my_bases )
			if( gs.getActionAssignment( u ) == null )
				baseBehavior( u, player, pgs, reserved_resources );

		for( Unit u : my_barracks )
			if( gs.getActionAssignment( u ) == null )
				barracksBehavior( u, player, gs, pgs, gs.getTime(), reserved_resources );

		for( Unit u : my_army )
			if( gs.getActionAssignment( u ) == null )
			{
				// BASIC BEHAVIOR
				armyUnitBehavior_heatmap( u, player, gs );
				
				// not BASIC BEHAVIOR
				// if( number_units_can_attack >= 4 )
				//	 armyUnitBehavior_heatmap(u, player, gs);
				// else
				//	 armyUnitBehavior(u, player, gs);
			}

		workersBehavior( player, gs, reserved_resources );

		// This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
		return translateActions( p, gs );
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
	protected void baseBehavior( Unit u, Player player, PhysicalGameState pgs, AtomicInteger reserved_resources )
	{
		int nb_workers = my_workers.size();
		if( scout_ID != -1 )
			--nb_workers;

		// BASIC BEHAVIOR
		// if( nb_workers < 1 && player.getResources() >= worker_type.cost )
		// 	train( u, worker_type );

		// not BASIC BEHAVIOR
		// train 1 worker for each resource patch, excluding the scout
		if( nb_workers < my_resource_patches.size() && player.getResources() >= worker_type.cost )
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

	protected void armyUnitCommonBehavior( Unit u, Player player, GameState gs, PhysicalGameState pgs, Unit closest_enemy )
	{
		int closest_distance = manhattanDistance( u, closest_enemy );
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
				for( Unit eu : enemy_army )
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

	protected void armyUnitBehavior_heatmap( Unit u, Player player, GameState gs )
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Unit closest_enemy = getClosestEnemy( u, player, pgs );

		if( closest_enemy != null )
			armyUnitCommonBehavior( u, player, gs, pgs, closest_enemy );
		else
			if( gs instanceof PartiallyObservableGameState )
			{
				// there are no enemies, so we need to explore (find the least known place):
				int min_x = 0;
				int min_y = 0;
				int heat_point = Integer.MAX_VALUE;

				for( int i = 0 ; i < pgs.getWidth() ; ++i )
					for( int j = 0 ; j < pgs.getHeight() ; ++j )
					{
						if( heat_map[i][j] < heat_point )
						{
							heat_point = heat_map[i][j];
							min_x = i;
							min_y = j;
						}
						else
							if( heat_map[i][j] == heat_point )
								if( Math.random() <= 0.5 )
								{
									heat_point = heat_map[i][j];
									min_x = i;
									min_y = j;
								}
					}

				move( u, min_x, min_y );
			}
	}

	protected void armyUnitBehavior( Unit u, Player player, GameState gs )
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Unit closest_enemy = getClosestEnemy( u, player, pgs );

		if( closest_enemy != null )
			armyUnitCommonBehavior( u, player, gs, pgs, closest_enemy );
		else
		{
			if( gs instanceof PartiallyObservableGameState )
			{
				PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
				// there are no enemies, so we need to explore (find the nearest non-observable place):
				int closest_x = 0;
				int closest_y = 0;
				int closest_distance = Integer.MAX_VALUE;

				for( int i = 0 ; i < pgs.getHeight() ; ++i )
					for( int j = 0 ; j < pgs.getWidth() ; ++j )
						if( !pogs.observable( j, i ) )
						{
							int d = ( u.getX() - j ) * ( u.getX() - j ) + ( u.getY() - i ) * ( u.getY() - i );
							if( d < closest_distance )
							{
								closest_x = j;
								closest_y = i;
								closest_distance = d;
							}
						}

				move( u, closest_x, closest_y );
			}
		}
	}

	protected void barracksBehavior( Unit u, Player player, GameState gs, PhysicalGameState pgs, int time, AtomicInteger reserved_resources )
	{
		int player_idle_barracks = 0;

		int sol_heavy = 0;
		int sol_ranged = 0;
		int sol_light = 0;

		time = time - ( time % 10 );
		if( player.getResources() >= 2 )
		{
			for( Unit b : my_barracks )
			{
				UnitAction ua = gs.getUnitAction( b );
				if( ua == null || ua.getType() == UnitAction.TYPE_NONE )
					++player_idle_barracks;
			}
				
			observed_worker =	Math.max( observed_worker, enemy_workers.size() );
			observed_heavy = Math.max( observed_heavy, enemy_heavy_units.size() );
			observed_ranged =	Math.max( observed_ranged, enemy_ranged_units.size() );
			observed_light = Math.max( observed_light, enemy_light_units.size() );

			if( enemy_army.size() > 0 || enemy_barracks.size() > 0 )
				enemy_has_barracks = true;

			// Draws
			ArrayList<Integer[]> samples = new ArrayList<Integer[]>();
			Double[] info = { 0.0, 0.0, 0.0, 0.0 };

			for( int i = 0 ; i <= nb_samples ; ++i )
			{
				Integer[] tmp = new Integer[4];
				if( enemy_has_barracks )
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

				writer.println( player_idle_barracks );
				writer_log.println( player_idle_barracks );

				writer.println( my_heavy_units.size() );
				writer.println( my_ranged_units.size() );
				writer.println( my_light_units.size() );
				writer_log.println( my_heavy_units.size() );
				writer_log.println( my_ranged_units.size() );
				writer_log.println( my_light_units.size() );

				// if( player.getResources() <= 2 )
				// 	writer.println( 3 );
				// else
				// 	writer.println( player.getResources() );

				writer.println( player.getResources() );
				writer_log.println( player.getResources() );

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

				BufferedReader buffer = new BufferedReader( new InputStreamReader( process.getInputStream() ) );

				sol_heavy = Integer.parseInt( buffer.readLine() );
				sol_ranged = Integer.parseInt( buffer.readLine() );
				sol_light = Integer.parseInt( buffer.readLine() );

				System.out.println( "H" + sol_heavy + " R" + sol_ranged + " L" + sol_light + "\n" );
				buffer.close();
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
						if( player.getResources() >= heavy_type.cost )
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
						if( player.getResources() >= heavy_type.cost )
						{
							train( u, heavy_type );
							reserved_resources.addAndGet( heavy_type.cost );							
						}
		}
	}

	protected void workersBehavior( Player player, GameState gs, AtomicInteger reserved_resources )
	{
		if( my_workers.isEmpty() )
			return;

		PhysicalGameState pgs = gs.getPhysicalGameState();

		List<Unit> free_workers = new ArrayList<Unit>();

		// BASIC BEHAVIOR
		free_workers.addAll( my_workers );

		// not BASIC BEHAVIOR
		// for( Unit w : my_workers )
		//	 if( w.getID() != scout_ID )
		//		 free_workers.add( w );
		//	 else
		//		 armyUnitBehavior_heatmap(w, player, gs);

		// // if our scout died
		// if( scout && scout_ID != -1 && my_workers.size() == free_workers.size() )
		//	 scout_ID = -1;

		// if( scout && scout_ID == -1 && !free_workers.isEmpty() && free_workers.get(0) != null )
		// {
		//	 Unit w = free_workers.remove(0);
		//	 scout_ID = w.getID();
		//	 armyUnitBehavior_heatmap(w, player, gs);
		// }

		List<Integer> reserved_positions = new LinkedList<Integer>();
		if( my_bases.size() == 0 && !free_workers.isEmpty() )
		{
			// build a base:
			if( player.getResources() >= base_type.cost + reserved_resources.get() )
			{
				Unit u = free_workers.remove( 0 );
				AtomicInteger new_building_x = new AtomicInteger( u.getX() );
				AtomicInteger new_building_y = new AtomicInteger( u.getY() );
				spiralSearch( new_building_x, new_building_y, gs );
				buildIfNotAlreadyBuilding( u, base_type, new_building_x.get(), new_building_y.get(), reserved_positions, player, pgs );
				reserved_resources.addAndGet( base_type.cost );
			}
		}

		if( my_barracks.size() == 0 )
		{
			// build a barracks:
			if( player.getResources() >= barracks_type.cost + reserved_resources.get() && !free_workers.isEmpty() )
			{
				Unit u = free_workers.remove( 0 );
				AtomicInteger new_building_x = new AtomicInteger( u.getX() );
				AtomicInteger new_building_y = new AtomicInteger( u.getY() );
				spiralSearch( new_building_x, new_building_y, gs );
				buildIfNotAlreadyBuilding( u, barracks_type, new_building_x.get(), new_building_y.get(), reserved_positions, player, pgs );
				reserved_resources.addAndGet( barracks_type.cost );
			}
		}

		// harvest with all the free workers:
		for( Unit u : free_workers )
		{
			Unit closest_base = null;
			Unit closest_resource = null;
			int closest_distance = Integer.MAX_VALUE;

			// Assign closest resource patches among mine
			for( Unit r : my_resource_patches )
			{
				int d = manhattanDistance( u, r );
				if( d < closest_distance )
				{
					closest_resource = r;
					closest_distance = d;
				}
			}

			// Assign closest resource patches we know
			if( closest_resource == null )
				for( Unit r : resource_patches )
				{
					int d = manhattanDistance( u, r );
					if( d < closest_distance )
					{
						closest_resource = r;
						closest_distance = d;
					}
				}

			// Search for resource patches
			if( closest_resource == null )
			{
				// not BASIC behavior
				//TODO
			}

			// Spot the closest base
			closest_distance = Integer.MAX_VALUE;
			for( Unit b : my_bases )
			{
				int d = manhattanDistance( u, b );
				if( d < closest_distance )
				{
					closest_base = b;
					closest_distance = d;
				}
			}

			if( closest_resource != null && closest_base != null )
					harvest( u, closest_resource, closest_base );
			
			// not BASIC behavior
			// explore if no resource around. Remember where were far resources.
		}
	}
}
