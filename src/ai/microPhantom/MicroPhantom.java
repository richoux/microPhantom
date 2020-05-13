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

	//public static PrintWriter writer_log;

	Player player;
	GameState gs;
	PhysicalGameState pgs;
	
	String solver_path;
	String solver_name;
	int[][] heat_map;

	int observed_worker;
	int observed_light;
	int observed_heavy;
	int observed_ranged;

	int initial_base_position_x;
	int initial_base_position_y;
	
	boolean random_version;
	boolean scout;
	long scout_ID;

	int number_units_can_attack;

	int map_width;
	int map_height;
	int map_surface;
	
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

	UnitType most_expensive_type;
	UnitType cheapest_type;
	// Don't delete: Not used yet, but can be useful
	// UnitType fastest_to_train_type;
	// UnitType longest_to_train_type;

	boolean enemy_has_barracks;

	/*
	 * Constructors
	 */
	public MicroPhantom( UnitTypeTable a_utt,
	                     String distribution_file_b,
	                     String distribution_file_wb,
	                     String solver_path )
	{
		this( a_utt, new AStarPathFinding(), distribution_file_b, distribution_file_wb, solver_path );
	}

	protected MicroPhantom( UnitTypeTable a_utt,
	                        PathFinding a_pf,
	                        String distribution_file_b,
	                        String distribution_file_wb,
	                        String solver_path,
	                        int[][] heat_map )
	{
		this( a_utt, a_pf, distribution_file_b, distribution_file_wb, solver_path );
		if( heat_map != null )
		{
			this.heat_map = new int[ heat_map.length ][];
			for( int y = 0 ; y < heat_map.length ; ++y )
				this.heat_map[y] = heat_map[y].clone();
		}
	}

	protected MicroPhantom( UnitTypeTable a_utt,
	                        PathFinding a_pf,
	                        String distribution_file_b,
	                        String distribution_file_wb,
	                        String solver_path )
	{
		super( a_pf );
		reset( a_utt );
		this.solver_path = solver_path;
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

		// try
		// {
		// 	//writer_log = new PrintWriter( "src/ai/microPhantom/solver.log", "UTF-8" );
		// 	writer_log = new PrintWriter( "heatmap.txt", "UTF-8" );
		// }
		// catch( IOException e1 )
		// {
		// 	System.out.println( "Exception with writer log" );
		// }

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
	private void initMapAnalysis()
	{
		map_width = pgs.getWidth();
		map_height = pgs.getHeight();
		map_surface = map_width * map_height;
		heat_map = new int[ map_height ][ map_width ];

		// Commented, because pf.pathExists behavior really is incomprehensible
		// Unit u;
		// if( !my_workers.isEmpty() )
		// 	u = my_workers.get( 0 );
		// else if( !my_bases.isEmpty() )
		// 	u = my_bases.get( 0 );
		// else
		// 	u = my_units.get( 0 );		
		
		if( gs instanceof PartiallyObservableGameState )
		{
			PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
			for( int y = 0 ; y < map_height ; ++y )
				for( int x = 0 ; x < map_width ; ++x )
				{
					int target = map_width * y + x;
					if( pgs.getTerrain( x, y ) == pgs.TERRAIN_WALL ) //|| pf.pathExists( u, target, gs, null ) )
						heat_map[y][x] = Integer.MAX_VALUE;
					else if( pogs.observable( x, y ) )
						heat_map[y][x] = gs.getTime();
					else
						heat_map[y][x] = -1;
				}
		}
	}

	private void updateHeatMap()
	{
		if( gs instanceof PartiallyObservableGameState )
		{
			PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
			for( int y = 0 ; y < map_height ; ++y )
				for( int x = 0 ; x < map_width ; ++x )
					if( pogs.observable( x, y ) && heat_map[y][x] < Integer.MAX_VALUE )
						heat_map[y][x] = gs.getTime();
		}
	}

	private void scanUnits()
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
					{
						if( initial_base_position_x == -1 )
						{
							initial_base_position_x = u.getX();
							initial_base_position_y = u.getY();
						}
						my_bases.add( u );
					}
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

	private boolean isPotentialThreat( Unit u )
	{
		return u != null && u.getPlayer() >= 0 && u.getPlayer() != player.getID() && u.getType().canAttack;
	}
	
	private double euclidianDistance( Unit u1, Unit u2 )
	{
		return Math.sqrt( Math.pow( u2.getX() - u1.getX(), 2 ) + Math.pow( u2.getY() - u1.getY(), 2 ) );
	}

	private double euclidianDistance( int x1, int y1, int x2, int y2 )
	{
		return Math.sqrt( Math.pow( x2 - x1, 2 ) + Math.pow( y2 - y1, 2 ) );
	}
	
	private int manhattanDistance( Unit u1, Unit u2 )
	{
		return Math.abs( u2.getX() - u1.getX() ) + Math.abs( u2.getY() - u1.getY() );
	}

		private int manhattanDistance( int x1, int y1, int x2, int y2 )
	{
		return Math.abs( x2 - x1 ) + Math.abs( y2 - y1 );
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

	private boolean notOnBorder( int x, int y )
	{
		return x + 1 < map_width && x - 1 >= 0 && y + 1 < map_height && y - 1 >= 0;
	}
	
	private boolean freeAround( int x, int y )
	{
		return notOnBorder( x, y )
			&& ( pgs.getUnitAt( x    , y     ) == null || pgs.getUnitAt( x    , y     ).getType().canMove )
			&& ( pgs.getUnitAt( x + 1, y     ) == null || pgs.getUnitAt( x + 1, y     ).getType().canMove )
			&& ( pgs.getUnitAt( x - 1, y     ) == null || pgs.getUnitAt( x - 1, y     ).getType().canMove )
			&& ( pgs.getUnitAt( x    , y + 1 ) == null || pgs.getUnitAt( x    , y + 1 ).getType().canMove )
			&& ( pgs.getUnitAt( x    , y - 1 ) == null || pgs.getUnitAt( x    , y - 1 ).getType().canMove )
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
	private void spiralSearch( AtomicInteger iX, AtomicInteger iY )
	{
		int x = iX.get();
		int y = iY.get();

		int step = 0;
		int target = 1;
		boolean x_turn = true;
		while( !freeAround( x, y ) )
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

	private Unit getClosestEnemy( Unit u )
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
		//writer_log.close();
		super.gameOver( winner );
	}

	public AI clone()
	{
		return new MicroPhantom( utt, pf, distribution_file_b, distribution_file_woutb, solver_path, heat_map );
	}

	@Override
	public void reset()
	{
		player = null;
		gs = null;
		pgs = null;

		heat_map = null;

		observed_worker = 0;
		observed_light = 0;
		observed_heavy = 0;
		observed_ranged = 0;

		initial_base_position_x = -1;
		initial_base_position_y = -1;

		random_version = false;
		scout = false;
		scout_ID = -1;

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

		nb_samples = NB_SAMPLES;
		
		base_type = utt.getUnitType( "Base" );
		barracks_type = utt.getUnitType( "Barracks" );
		worker_type = utt.getUnitType( "Worker" );
		heavy_type = utt.getUnitType( "Heavy" );
		light_type = utt.getUnitType( "Light" );
		ranged_type = utt.getUnitType( "Ranged" );

		if( heavy_type.cost >= light_type.cost )
		{
			if( heavy_type.cost >= ranged_type.cost )
				most_expensive_type = heavy_type;
			else
				most_expensive_type = ranged_type;

			if( ranged_type.cost >= light_type.cost )
				cheapest_type = light_type;
			else
				cheapest_type = ranged_type;
		}
		else
		{
			if( ranged_type.cost >= light_type.cost )
				most_expensive_type = ranged_type;
			else
				most_expensive_type = light_type;

			if( heavy_type.cost >= ranged_type.cost )
				cheapest_type = ranged_type;
			else
				cheapest_type = heavy_type;
		}

		// Don't delete: Not used yet, but can be useful
		// if( heavy_type.produceTime >= light_type.produceTime )
		// {
		// 	if( heavy_type.produceTime >= ranged_type.produceTime )
		// 		longest_to_train_type = heavy_type;
		// 	else
		// 		longest_to_train_type = ranged_type;

		// 	if( ranged_type.produceTime >= light_type.produceTime )
		// 		fastest_to_train_type = light_type;
		// 	else
		// 		fastest_to_train_type = ranged_type;
		// }
		// else
		// {
		// 	if( ranged_type.produceTime >= light_type.produceTime )
		// 		longest_to_train_type = ranged_type;
		// 	else
		// 		longest_to_train_type = light_type;

		// 	if( heavy_type.produceTime >= ranged_type.produceTime )
		// 		fastest_to_train_type = ranged_type;
		// 	else
		// 		fastest_to_train_type = heavy_type;
		// }
				
		enemy_has_barracks = false;

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
	public PlayerAction getAction( int p, GameState game_state )
	{
		gs = game_state;
		pgs = gs.getPhysicalGameState();

		if( player == null )
			player = gs.getPlayer( p );

		scanUnits();

		if( heat_map == null )
			initMapAnalysis();
		else
			updateHeatMap();
		
		double distance_threshold = Math.max( Math.sqrt( map_surface ) / 4, worker_type.sightRadius );

		// determine how many resource patches I have near my bases, given a distance threshold
		my_resource_patches.clear();
		number_units_can_attack = 0;
		AtomicInteger reserved_resources = new AtomicInteger( 0 );

		// if( gs.getTime() % 500 == 0 )
		// {
		// 	writer_log.println( "\n\nTime: " + gs.getTime() );
		// 	for( int y = 0 ; y < map_height ; ++y )
		// 	{
		// 		for( int x = 0 ; x < map_width ; ++x )
		// 		{
		// 			String heat;
		// 			if( heat_map[y][x] < Integer.MAX_VALUE )
		// 				heat = String.format( "%-4s ", heat_map[y][x] );
		// 			else
		// 			{
		// 				int wall = -10;
		// 				heat = String.format( "%-4s ", wall );
		// 			}
		// 			writer_log.print( heat );
		// 		}
		// 		writer_log.println("");
		// 	}
		// }
		
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
				baseBehavior( u, reserved_resources );

		for( Unit u : my_barracks )
			if( gs.getActionAssignment( u ) == null )
				barracksBehavior( u, reserved_resources );

		for( Unit u : my_army )
			if( gs.getActionAssignment( u ) == null )
			{
				// BASIC BEHAVIOR
				armyUnitBehavior_heatmap( u );
				
				// not BASIC BEHAVIOR
				// if( number_units_can_attack >= 4 )
				//	 armyUnitBehavior_heatmap( u );
				// else
				//	 armyUnitBehavior( u );
			}

		workersBehavior( reserved_resources );

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
	protected boolean moveIfPathExists( Unit u, int x, int y )
	{
		int target = map_width * y + x;
		if( pf.pathExists( u, target, gs, null ) )
		{
			super.move( u, x, y );
			return true;
		}
		else
			return false;
	}

	protected void baseBehavior( Unit u, AtomicInteger reserved_resources )
	{
		int nb_workers = my_workers.size();
		if( scout_ID != -1 )
			--nb_workers;

		// BASIC BEHAVIOR
		// if( nb_workers < 1 && player.getResources() >= worker_type.cost )
		// 	train( u, worker_type );

		// not BASIC BEHAVIOR
		// train 1 worker for each resource patch, excluding the scout
		if( ( nb_workers < my_resource_patches.size() || nb_workers <= 0 ) && player.getResources() >= worker_type.cost && nb_workers < 4 )
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

	protected void armyUnitCommonBehavior( Unit u, Unit closest_enemy )
	{
		double closest_distance = euclidianDistance( u, closest_enemy );
		if( u.getType().ID == ranged_type.ID && closest_distance < 2 )
		{
			if( closest_enemy.getType().ID == ranged_type.ID ) //|| closest_enemy_action == null || ( closest_enemy_action.action.getType() != UnitAction.TYPE_MOVE && closest_distance > 1 ) )
				attack( u, closest_enemy );
			else
			{
				// hit 'n run behavior for ranged units
				// we compute for each possible position the danger level
				int[] danger = new int[4];
				final int UP = 0;
				final int RIGHT = 1;
				final int DOWN = 2;
				final int LEFT = 3;
				
				int x = u.getX();
				int y = u.getY();
				
				// count enemy units at positions around us
				if( isPotentialThreat( pgs.getUnitAt( x - 1, y - 1 ) ) )
				{
					++danger[UP];
					++danger[LEFT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x    , y - 1 ) ) )
				{
					++danger[UP];
				}
				if( isPotentialThreat( pgs.getUnitAt( x + 1, y - 1 ) ) )
				{
					++danger[UP];
					++danger[RIGHT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x - 1, y     ) ) )
				{
					++danger[LEFT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x + 1, y     ) ) )
				{
					++danger[RIGHT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x - 1, y + 1 ) ) )
				{
					++danger[DOWN];
					++danger[LEFT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x    , y + 1 ) ) )
				{
					++danger[DOWN];
				}
				if( isPotentialThreat( pgs.getUnitAt( x + 1, y + 1 ) ) )
				{
					++danger[DOWN];
					++danger[RIGHT];
				}

				int index_min = -1;
				if( danger[UP] + danger[RIGHT] + danger[DOWN] + danger[LEFT] > 0 )
				{
					int min = Integer.MAX_VALUE;
					for( int i = 0 ; i < 4 ; ++i )
					{
						if( danger[i] < min )
						{
							min = danger[i];
							index_min = i;
						}
					}
				}

				switch( index_min )
				{
				case UP:
					move( u, x    , y - 1 );
					break;
				case RIGHT:
					move( u, x + 1, y     );
					break;
				case DOWN:
					move( u, x    , y - 1 );
					break;
				case LEFT:
					move( u, x - 1, y     );
					break;
				default:
					attack( u, closest_enemy );
					break;					
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

				if( manhattanDistance( x, y, u.getX(), u.getY() ) <= manhattanDistance( u, closest_enemy ) )
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

	protected void armyUnitBehavior_heatmap( Unit u )
	{
		Unit closest_enemy = getClosestEnemy( u );

		if( closest_enemy != null )
			armyUnitCommonBehavior( u, closest_enemy );
		else
			if( gs instanceof PartiallyObservableGameState )
			{
				// there are no enemies, so we need to explore (find the least known place):
				int min_x = 0;
				int min_y = 0;
				int heat_point = Integer.MAX_VALUE;
				double tiebreak_distance = Double.MAX_VALUE;
				
				for( int y = 0 ; y < map_height ; ++y )
					for( int x = 0 ; x < map_width ; ++x )
					{
						if( heat_map[y][x] < heat_point )
						{
							heat_point = heat_map[y][x];
							min_x = x;
							min_y = y;
						}
						else
							if( heat_map[y][x] == heat_point )
							{
								// as a tiebreaker, take the point closest to the mirror position of our base, if any
								if( initial_base_position_x != -1 )
								{
									double distance = euclidianDistance( map_width - initial_base_position_x, map_height - initial_base_position_y, x, y );
									if( distance < tiebreak_distance )
									{
										tiebreak_distance = distance;
										min_x = x;
										min_y = y;
									}
								}
								else
									if( Math.random() <= 0.5 )
									{
										heat_point = heat_map[y][x];
										min_x = x;
										min_y = y;
									}
							}
					}
				// if( pgs.getUnitAt( x, y ) != null && pgs.getUnitAt( x, y ).getType().isResource )
				// 	heat_map[y][x] = gs.getTime();
							
				System.out.println( "Unit " + u.getType().name + " num. " + u.getID() + ", currently at (" + u.getX() + "," + u.getY() + "), moves to (" + min_x + "," + min_y + ")" );
				//if( !moveIfPathExists( u, min_x, min_y ) )
				// 	heat_map[min_y][min_x] = Integer.MAX_VALUE;
				move( u, min_x, min_y );
			}
	}

	protected void armyUnitBehavior( Unit u )
	{
		Unit closest_enemy = getClosestEnemy( u );

		if( closest_enemy != null )
			armyUnitCommonBehavior( u, closest_enemy );
		else
		{
			if( gs instanceof PartiallyObservableGameState )
			{
				PartiallyObservableGameState pogs = (PartiallyObservableGameState)gs;
				// there are no enemies, so we need to explore (find the nearest non-observable place):
				int closest_x = 0;
				int closest_y = 0;
				int closest_distance = Integer.MAX_VALUE;

				for( int y = 0 ; y < map_height ; ++y )
					for( int x = 0 ; x < map_width ; ++x )
						if( pgs.getTerrain( x, y ) == pgs.TERRAIN_NONE || pgs.getUnitAt( x, y ) == null || !pgs.getUnitAt( x, y ).getType().isResource )
						{
							if( !pogs.observable( x, y ) )
							{
								int d = ( u.getX() - x ) * ( u.getX() - x ) + ( u.getY() - y ) * ( u.getY() - y );
								if( d < closest_distance )
								{
									closest_x = x;
									closest_y = y;
									closest_distance = d;
								}
							}
						}

				move( u, closest_x, closest_y );
				// If no paths exist to go to this (x,y) position, move randomly
				// if( !moveIfPathExists( u, closest_x, closest_y ) )
				// 	move( u, (int)(map_width * Math.random() ), (int)(map_height * Math.random() ) );
			}
		}
	}

	protected void barracksBehavior( Unit u, AtomicInteger reserved_resources )
	{
		int player_idle_barracks = 0;

		int sol_heavy = 0;
		int sol_ranged = 0;
		int sol_light = 0;

		int time = gs.getTime();
		time -= ( time % 10 );
		if( player.getResources() >= cheapest_type.cost )
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
				// writer_log.println( "Time: " + time );

				// Samples indexes:
				// 0 for worker
				// 1 for heavy
				// 2 for ranged
				// 3 for light
				for( int i = 0 ; i < nb_samples ; ++i )
				{
					writer.println( samples.get(i)[0] + " " + samples.get(i)[1] + " " + samples.get(i)[2] + " " + samples.get(i)[3] );
					// writer_log.println( samples.get(i)[0] + " " + samples.get(i)[1] + " " + samples.get(i)[2] + " " + samples.get(i)[3] );
				}

				writer.println( heavy_type.cost );
				writer.println( ranged_type.cost );
				writer.println( light_type.cost );
				// writer_log.println( heavy_type.cost );
				// writer_log.println( ranged_type.cost );
				// writer_log.println( light_type.cost );
				
				writer.println( heavy_type.produceTime );
				writer.println( ranged_type.produceTime );
				writer.println( light_type.produceTime );
				// writer_log.println( heavy_type.produceTime );
				// writer_log.println( ranged_type.produceTime );
				// writer_log.println( light_type.produceTime );

				writer.println( my_heavy_units.size() );
				writer.println( my_ranged_units.size() );
				writer.println( my_light_units.size() );
				// writer_log.println( my_heavy_units.size() );
				// writer_log.println( my_ranged_units.size() );
				// writer_log.println( my_light_units.size() );

				writer.println( player_idle_barracks );
				// writer_log.println( player_idle_barracks );

				writer.println( player.getResources() );
				// writer_log.println( player.getResources() );

				writer.println( map_surface );
				// writer_log.println( map_surface );

				// writer_log.println("#########\n");
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

				if( map_surface >= 400 )
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
					if( sol_light >= sol_heavy && player.getResources() >= light_type.cost )
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
					if( sol_ranged >= sol_heavy && player.getResources() >= ranged_type.cost)
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

	protected void workersBehavior( AtomicInteger reserved_resources )
	{
		if( my_workers.isEmpty() )
			return;

		List<Unit> free_workers = new ArrayList<Unit>();

		// BASIC BEHAVIOR
		free_workers.addAll( my_workers );

		// not BASIC BEHAVIOR
		// for( Unit w : my_workers )
		//	 if( w.getID() != scout_ID )
		//		 free_workers.add( w );
		//	 else
		//		 armyUnitBehavior_heatmap( w );

		// // if our scout died
		// if( scout && scout_ID != -1 && my_workers.size() == free_workers.size() )
		//	 scout_ID = -1;

		// if( scout && scout_ID == -1 && !free_workers.isEmpty() && free_workers.get(0) != null )
		// {
		//	 Unit w = free_workers.remove(0);
		//	 scout_ID = w.getID();
		//	 armyUnitBehavior_heatmap( w );
		// }

		List<Integer> reserved_positions = new LinkedList<Integer>();
		if( my_bases.size() == 0 && !free_workers.isEmpty() )
		{
			// build a base, and don't count reserved_resources: it's top priority
			if( player.getResources() >= base_type.cost )
			{
				Unit u = free_workers.remove( 0 );
				AtomicInteger new_building_x = new AtomicInteger( u.getX() );
				AtomicInteger new_building_y = new AtomicInteger( u.getY() );
				spiralSearch( new_building_x, new_building_y );
				buildIfNotAlreadyBuilding( u, base_type, new_building_x.get(), new_building_y.get(), reserved_positions, player, pgs );
				reserved_resources.addAndGet( base_type.cost );
			}
		}

		// if no barracks or plainty of money (on maps larger than 12x12)
		if( my_barracks.size() == 0 || ( player.getResources() >= barracks_type.cost + reserved_resources.get() + most_expensive_type.cost && map_surface > 144 ) )
		{
			// build a barracks:
			if( player.getResources() >= barracks_type.cost + reserved_resources.get() && !free_workers.isEmpty() )
			{
				// get the worker the farther away from a barracks, if any
				Unit u = free_workers.get( 0 ) ;
				if( my_barracks.size() == 0 )
					u = free_workers.remove( 0 );
				else
				{
					double distance_from_barracks = -1;
					for( Unit w : free_workers )
						for( Unit b : my_barracks )
						{
							double distance = euclidianDistance( w, b );
							if( distance > distance_from_barracks )
							{
								distance = distance_from_barracks;
								u = w;
							}
						}
					free_workers.remove( u );
				}

				// on very small maps, build anywhere
				// otherwise, find a good place for the barracks
				if( map_surface <= 144 )
				{
					buildIfNotAlreadyBuilding( u, barracks_type, u.getX(), u.getY(), reserved_positions, player, pgs );
				}
				else
				{
					AtomicInteger new_building_x = new AtomicInteger( u.getX() );
					AtomicInteger new_building_y = new AtomicInteger( u.getY() );
					spiralSearch( new_building_x, new_building_y );
					buildIfNotAlreadyBuilding( u, barracks_type, new_building_x.get(), new_building_y.get(), reserved_positions, player, pgs );
				}
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
