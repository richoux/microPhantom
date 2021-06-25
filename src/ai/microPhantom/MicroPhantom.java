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

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.NumberFormatException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.microphantom.protos.SolutionBuffer;
import com.microphantom.protos.GameStateBuffer;

/**
 * @author Florian Richoux
 * (based upon POAdaptive by Valentin Antuari)
 */
public class MicroPhantom extends AbstractionLayerAI
{
	protected UnitTypeTable utt;

	public static int NB_SAMPLES = 50;

	// public static PrintWriter writer_log;

	Player player;
	GameState gs;
	PhysicalGameState pgs;
	PartiallyObservableGameState pogs;
	
	String solver_path;
	int solver_type;
	int[][] heat_map;

	int observed_worker;
	int observed_heavy;
	int observed_light;
	int observed_ranged;

	int observed_worker_in_total;
	int observed_heavy_in_total;
	int observed_light_in_total;
	int observed_ranged_in_total;

	int initial_base_position_x;
	int initial_base_position_y;
	int initial_number_workers;
	int min_distance_resource_base;
	int max_distance_resource_base;
	boolean has_initial_base;
	boolean has_initial_barracks;

	int initial_resources;
	
	boolean random_version;
	boolean scout;
	long scout_ID;

	int number_units_can_attack;
	int number_idle_barracks;
	
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
	int number_heavy_to_produce;
	int number_light_to_produce;
	int number_ranged_to_produce;
	boolean no_training;
	
	class TrackUnit
	{
		public Unit unit;
		public boolean alive;

		public TrackUnit( Unit unit, boolean alive )
		{
			this.unit = unit;
			this.alive = alive;
		}
	}

	HashMap<Long, TrackUnit> track_my_army;
	HashMap<Long, TrackUnit> track_enemy;
	HashMap<Integer, AtomicInteger> count_current_enemy; // observed, of course
	HashMap<Integer, AtomicInteger> count_total_enemy;   // observed, of course

	int my_cost_loss;
	int enemy_cost_loss;
	
	UnitType base_type;
	UnitType barracks_type;
	UnitType worker_type;
	UnitType heavy_type;
	UnitType light_type;
	UnitType ranged_type;

	UnitType most_expensive_type;
	UnitType cheapest_type;
	UnitType fastest_to_train_type;
	UnitType slowest_to_train_type;

	ServerSocketChannel serverSocketChannel;
	
	/*
	 * Constructors
	 */
	public MicroPhantom( UnitTypeTable a_utt,
	                     String solver_path )
	{
		this( a_utt, new AStarPathFinding(), solver_path );
	}

	protected MicroPhantom( UnitTypeTable a_utt,
	                        PathFinding a_pf,
	                        String solver_path,
	                        int[][] heat_map )
	{
		this( a_utt, a_pf, solver_path );
		if( heat_map != null )
		{
			this.heat_map = new int[ heat_map.length ][];
			for( int y = 0 ; y < heat_map.length ; ++y )
				this.heat_map[y] = heat_map[y].clone();
		}
	}

	protected MicroPhantom( UnitTypeTable a_utt,
	                        PathFinding a_pf,
	                        String solver_path )
	{
		super( a_pf );
		reset( a_utt );
		this.solver_path = solver_path;

		try
		{
			InetAddress inetAddress = InetAddress.getByName( "localhost" );  
			int port = 1085;
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.bind( new InetSocketAddress( inetAddress, port ) );
		}
		catch( IOException e1 )
		{
			System.out.println( "IO exception in process" );
			System.out.println( e1.getMessage() );
		}
		
		// try
		// {
		// 	writer_log = new PrintWriter( "heatmap.txt", "UTF-8" );
		// }
		// catch( IOException e1 )
		// {
		// 	System.out.println( "Exception with writer log" );
		// }
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
		
		if( pogs != null )
		{
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
		if( pogs != null )
		{
			for( int y = 0 ; y < map_height ; ++y )
				for( int x = 0 ; x < map_width ; ++x )
					if( pogs.observable( x, y ) && heat_map[y][x] < Integer.MAX_VALUE )
						heat_map[y][x] = gs.getTime();
		}
	}

	private void scanUnits()
	{
		// remove empty resource patches
		Iterator<Unit> iter = resource_patches.iterator();
		while( iter.hasNext() )
		{
			Unit r = iter.next();
			if( r.getResources() <= 0 )
			{
				iter.remove();
				// System.out.println( "Resource patch at " + r.getX() + "," + r.getY() + " is now empty." );
			}
		}
		
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

		number_units_can_attack = 0;
		number_idle_barracks = 0;
		
		for( Unit u : pgs.getUnits() )
		{
			if( u.getType().isResource && !resource_patches.contains( u ) )
			{
				resource_patches.add( u );
				// System.out.println( "New resource patch at " + u.getX() + "," + u.getY() + "!" );
			}
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
							has_initial_base = true;
						}
						my_bases.add( u );
					}
					else if( u.getType().ID == barracks_type.ID )
					{
						my_barracks.add( u );
						if( gs.getUnitAction( u ) == null )
							++number_idle_barracks;
					}
					else
					{
						if( u.getType().ID == worker_type.ID )
							my_workers.add( u );
						else
						{
							if( gs.getUnitAction( u ) == null )
								++number_units_can_attack;

							track_my_army.put( u.getID(), new TrackUnit( u, true ) );
							my_army.add( u );
							
							if( u.getType().ID == heavy_type.ID )
							{
								my_heavy_units.add( u );
								my_melee_units.add( u );
							}
							else if( u.getType().ID == light_type.ID )
							{
								my_light_units.add( u );
								my_melee_units.add( u );
							}
							else // must be a ranged unit
							{
								my_ranged_units.add( u );
							}
						}
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
						else
						{
							// If it is a unit we never saw before, count it.
							if( !track_enemy.containsKey( u.getID() ) )
							{
								if( count_current_enemy.containsKey( u.getType().ID ) )
								{
									count_current_enemy.get( u.getType().ID ).incrementAndGet();
									count_total_enemy.get( u.getType().ID ).incrementAndGet();
								}
								else
								{
									count_current_enemy.put( u.getType().ID, new AtomicInteger( 1 ) );
									count_total_enemy.put( u.getType().ID, new AtomicInteger( 1 ) );
								}
							}
							
							track_enemy.put( u.getID(), new TrackUnit( u, true ) );

							if( u.getType().ID == worker_type.ID )
								enemy_workers.add( u );
							else
							{
								enemy_army.add( u );
								
								if( u.getType().ID == heavy_type.ID )
								{
									enemy_heavy_units.add( u );
									enemy_melee_units.add( u );
								}
								else if( u.getType().ID == light_type.ID )
								{
									enemy_light_units.add( u );
									enemy_melee_units.add( u );
								}
								else // must be a ranged unit
									enemy_ranged_units.add( u );
							}
						}
					}
			}
		}

		if( initial_number_workers == -1 )
			initial_number_workers = my_workers.size();
		
		for( Map.Entry my_track : track_my_army.entrySet() )
		{
			TrackUnit track = (TrackUnit)my_track.getValue();
			// If this unit has been destroyed
			if( track.unit.getHitPoints() <= 0 && track.alive )
			{
				my_cost_loss += track.unit.getType().cost;
				track.alive = false;
			}
		}

		for( Map.Entry enemy_track : track_enemy.entrySet() )
		{
			TrackUnit track = (TrackUnit)enemy_track.getValue();
			// If this unit has been destroyed
			if( track.unit.getHitPoints() <= 0 && track.alive )
			{
				if( track.unit.getType().ID != worker_type.ID )
					enemy_cost_loss += track.unit.getType().cost;
				track.alive = false;
				count_current_enemy.get( track.unit.getType().ID ).decrementAndGet();
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

	private boolean reveal_enough_fog( Unit u, int x, int y )
	{
		int sight = u.getType().sightRadius;
		int count = 0;
		
		for( int y_sight = y - sight ; y_sight <= y + sight ; ++y_sight )
			for( int x_sight = x - sight ; x_sight <= x + sight ; ++x_sight )
				if( y_sight >= 0 && y_sight < map_height && x_sight >= 0 && x_sight < map_width && manhattanDistance( x, y, x_sight, y_sight ) <= sight && heat_map[y_sight][x_sight] == -1 )
					++count;

		if( gs.getTime() < 2000 )
			return count >= 12;
		else
			return count > 0;
	}

	// Search the closest coordinates (x,y) to the initial base revealing at least one case of fog
	// Tiebreaker: closest point to self
	private void searchResources( Unit u )
	{
		int move_x = u.getX() + (int)( 20 * Math.random() - 10 );
		int move_y = u.getY() + (int)( 20 * Math.random() - 10 );

		if( initial_base_position_x != -1 )
		{
			int distance_base = Integer.MAX_VALUE;
			int distance_self = Integer.MAX_VALUE;
			for( int x = 0 ; x < map_width ; ++x )
				for( int y = 0 ; y < map_height ; ++y )
					if( reveal_enough_fog( u, x, y ) )
					{
						int distance_base_tiebreak = manhattanDistance( initial_base_position_x, initial_base_position_y, x, y );
						if( distance_base_tiebreak < distance_base )
						{
							distance_base = distance_base_tiebreak;
							move_x = x;
							move_y = y;
						}
						else if( distance_base_tiebreak == distance_base )
						{
							int distance_self_tiebreak = manhattanDistance( u.getX(), u.getY(), x, y );
							if( distance_self_tiebreak < distance_self )
							{
								distance_self = distance_self_tiebreak;
								move_x = x;
								move_y = y;
							}
						}
					}
		}

		move( u, move_x, move_y );
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
		return new MicroPhantom( utt, pf, solver_path, heat_map );
	}

	@Override
	public void reset()
	{
		player = null;
		gs = null;
		pgs = null;
		pogs = null;

		solver_type = -1;
		heat_map = null;

		observed_worker = 0;
		observed_heavy = 0;
		observed_light = 0;
		observed_ranged = 0;

		observed_worker_in_total = 0;
		observed_heavy_in_total = 0;
		observed_light_in_total = 0;
		observed_ranged_in_total = 0;

		initial_base_position_x = -1;
		initial_base_position_y = -1;
		initial_number_workers = -1;
		min_distance_resource_base = -1;
		max_distance_resource_base = -1;
		has_initial_base = false;
		has_initial_barracks = false;

		initial_resources = 0;
		
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

		track_my_army = new HashMap<Long, TrackUnit>();
		track_enemy = new HashMap<Long, TrackUnit>();		
		count_current_enemy = new HashMap<Integer, AtomicInteger>();
		count_total_enemy = new HashMap<Integer, AtomicInteger>();
		my_cost_loss = 0;
		enemy_cost_loss = 0;
		
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

		if( heavy_type.produceTime >= light_type.produceTime )
		{
			if( heavy_type.produceTime >= ranged_type.produceTime )
				slowest_to_train_type = heavy_type;
			else
				slowest_to_train_type = ranged_type;

			if( ranged_type.produceTime >= light_type.produceTime )
				fastest_to_train_type = light_type;
			else
				fastest_to_train_type = ranged_type;
		}
		else
		{
			if( ranged_type.produceTime >= light_type.produceTime )
				slowest_to_train_type = ranged_type;
			else
				slowest_to_train_type = light_type;

			if( heavy_type.produceTime >= ranged_type.produceTime )
				fastest_to_train_type = ranged_type;
			else
				fastest_to_train_type = heavy_type;
		}
			
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
		if( gs instanceof PartiallyObservableGameState )
			pogs = (PartiallyObservableGameState)gs;

		if( player == null )
			player = gs.getPlayer( p );

		scanUnits();
		
		if( heat_map == null )
		{
			initMapAnalysis();
			if( !my_barracks.isEmpty() )
				has_initial_barracks = true;
			initial_resources = player.getResources();
		}
		else
			updateHeatMap();

		// if( gs.getTime() == 1000 )
		// 	writer_log.close();

		// if( gs.getTime() % 100 == 0 )
		// {
		// 	writer_log.println( "Time: " + gs.getTime() );
		// 	for( int y = 0 ; y < map_height ; ++y )
		// 	{
		// 		for( int x = 0 ; x < map_width ; ++x )
		// 		{
		// 			String heat;
		// 			if( heat_map[y][x] < Integer.MAX_VALUE )
		// 				heat = String.format( "%-3s ", heat_map[y][x] );
		// 			else
		// 			{
		// 				int wall = -10;
		// 				heat = String.format( "%-3s ", wall );
		// 			}
		// 			writer_log.print( heat );
		// 		}
		// 		writer_log.println("\n");
		// 	}
		// }
		
		double distance_threshold = Math.max( Math.sqrt( map_surface ) / 4, worker_type.sightRadius );

		// determine how many resource patches I have near my bases, given a distance threshold
		my_resource_patches.clear();
		AtomicInteger reserved_resources = new AtomicInteger( 0 );

		for( Unit u : resource_patches )
		{
			for( Unit b : my_bases )
				if( euclidianDistance( u, b ) <= distance_threshold )
				{
					// System.out.println( "My resource patch found at " + u.getX() + "," + u.getY() );
					my_resource_patches.add( u );
					break; // don't check it for another base
				}
		}

		if( min_distance_resource_base == -1 && !my_bases.isEmpty() )
		{
			int min_distance = Integer.MAX_VALUE;
			for( Unit r : my_resource_patches )
			{
				int d = manhattanDistance( my_bases.get( 0 ), r );
				if( d < min_distance )
					min_distance = d;
				if ( d > max_distance_resource_base )
					max_distance_resource_base = d;
			}
			if( min_distance < Integer.MAX_VALUE )
				min_distance_resource_base = min_distance;
		}
		
		for( Unit u : my_bases )
			if( gs.getUnitAction( u ) == null )
				baseBehavior( u, reserved_resources );

		if( number_idle_barracks > 0 )
			decideProduction();

		for( Unit u : my_barracks )
			if( gs.getUnitAction( u ) == null )
				barracksBehavior( u, reserved_resources );

		for( Unit u : my_army )
			if( gs.getUnitAction( u ) == null )
			{
				// BASIC BEHAVIOR
				// armyUnitBehavior_heatmap( u );
				
				// not BASIC BEHAVIOR
				//if( number_units_can_attack >= 4 )
				if( my_army.size() >= 3 )
					armyUnitBehavior_heatmap( u );
				else
					armyUnitBehavior( u );
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
		
		// If I have no workers or
		//  I have less workers than resource patches
		//  AND I have enough money to buy a worker
		//  AND I have less than 4 workers
		//  AND I have at least one barrack, or no barracks but not enough money to get one <== disabled now
		if( nb_workers <= 0 ||
		    ( nb_workers < my_resource_patches.size()
		      && player.getResources() >= worker_type.cost
		      && nb_workers < 4 ) )
			//&& !( my_barracks.isEmpty() && player.getResources() >= barracks_type.cost ) ) )
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
		//System.out.println("armyUnitCommonBehavior for unit " + u.getType() + " " + u.getID() );

		int closest_distance = manhattanDistance( u, closest_enemy );
		if( u.getType().ID == ranged_type.ID && closest_distance <= 2 )
		{
			if( closest_enemy.getType().ID == ranged_type.ID )
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
				if( isPotentialThreat( pgs.getUnitAt( x    , y - 1 ) ) || isPotentialThreat( pgs.getUnitAt( x    , y - 2 ) ) )
				{
					++danger[UP];
				}
				if( isPotentialThreat( pgs.getUnitAt( x + 1, y - 1 ) ) )
				{
					++danger[UP];
					++danger[RIGHT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x - 1, y     ) ) || isPotentialThreat( pgs.getUnitAt( x - 2, y     ) ) )
				{
					++danger[LEFT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x + 1, y     ) ) || isPotentialThreat( pgs.getUnitAt( x + 2, y     ) ) )
				{
					++danger[RIGHT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x - 1, y + 1 ) ) )
				{
					++danger[DOWN];
					++danger[LEFT];
				}
				if( isPotentialThreat( pgs.getUnitAt( x    , y + 1 ) ) || isPotentialThreat( pgs.getUnitAt( x    , y + 2 ) ) )
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
		//System.out.println("armyUnitBehavior_heatmap for unit " + u.getType() + " " + u.getID() );

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
				
				// Visit first the point closest to the mirror position of our base, if any
				if( initial_base_position_x != -1 && heat_map[map_height - 1 - initial_base_position_y][map_width - 1 - initial_base_position_x] == -1 )
				{
					min_x = map_width - 1 - initial_base_position_x;
					min_y = map_height - 1 - initial_base_position_y;
				}
				else
				{				
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
									// as a tiebreaker, take the point closest to the unit
									double distance = euclidianDistance( u.getX(), u.getY(), x, y );
									if( distance < tiebreak_distance && distance > 0 )
									{
										tiebreak_distance = distance;
										min_x = x;
										min_y = y;
									}
								}
						}
				}
				
				move( u, min_x, min_y );
			}
	}

	protected void armyUnitBehavior( Unit u )
	{
		//System.out.println("armyUnitBehavior for unit " + u.getType() + " " + u.getID() );
		Unit closest_enemy = getClosestEnemy( u );

		if( closest_enemy != null )
			armyUnitCommonBehavior( u, closest_enemy );
		else
		{
			if( pogs != null )
			{
				// there are no enemies, so we say in defense in front of the base
				int guess_enemy_base_x = 0;
				int guess_enemy_base_y = 0;

				if( initial_base_position_x != -1 )
				{
					// guess a mirror position
					guess_enemy_base_x = map_width - 1 - initial_base_position_x;
					guess_enemy_base_y = map_height - 1 - initial_base_position_y;
				}

				int direction_x;
				int direction_y;
				
				if( guess_enemy_base_x > initial_base_position_x )
					direction_x = 1;
				else
				{
					if( guess_enemy_base_x < initial_base_position_x )
						direction_x = 1;
					else
						direction_x = 0;
				}

				if( guess_enemy_base_y > initial_base_position_y )
					direction_y = 1;
				else
				{
					if( guess_enemy_base_y < initial_base_position_y )
						direction_y = 1;
					else
						direction_y = 0;
				}

				int max_map;
				if( map_width > map_height )
					max_map = map_width;
				else 
					max_map = map_height;
				
				move( u, initial_base_position_x + ( direction_x * ( max_map / 4 ) ), initial_base_position_y + ( direction_y * ( max_map / 4 ) ) );
			}
		}
	}

	protected void decideProduction()
	{
		if( count_current_enemy.get( worker_type.ID ) != null )
			observed_worker = count_current_enemy.get( worker_type.ID ).get();
		else
			observed_worker = 0;

		if( count_current_enemy.get( heavy_type.ID ) != null )
			observed_heavy = count_current_enemy.get( heavy_type.ID ).get();
		else
			observed_heavy = 0;

		if( count_current_enemy.get( light_type.ID ) != null )
			observed_light = count_current_enemy.get( light_type.ID ).get();
		else
			observed_light = 0;

		if( count_current_enemy.get( ranged_type.ID ) != null )
			observed_ranged =	count_current_enemy.get( ranged_type.ID ).get();
		else
			observed_ranged =	0;

		if( count_total_enemy.get( worker_type.ID ) != null )
			observed_worker_in_total = count_total_enemy.get( worker_type.ID ).get();
		else
			observed_worker_in_total = 0;

		if( count_total_enemy.get( heavy_type.ID ) != null )
			observed_heavy_in_total = count_total_enemy.get( heavy_type.ID ).get();
		else
			observed_heavy_in_total = 0;

		if( count_total_enemy.get( light_type.ID ) != null )
			observed_light_in_total = count_total_enemy.get( light_type.ID ).get();
		else
			observed_light_in_total = 0;

		if( count_total_enemy.get( ranged_type.ID ) != null )
			observed_ranged_in_total =	count_total_enemy.get( ranged_type.ID ).get();
		else
			observed_ranged_in_total =	0;

		try
		{
			int no_initial_base_int = has_initial_base ? 0 : 1;
			int no_initial_barracks_int = has_initial_barracks ? 0 : 1;

			if( my_cost_loss + 2 * cheapest_type.cost <= enemy_cost_loss )
				solver_type = 1;
			else if( my_cost_loss >= enemy_cost_loss + 2 * cheapest_type.cost )
				solver_type = 2;
			else
				solver_type = 0;

			no_training = false;
			Runtime r = Runtime.getRuntime();
			Process process = r.exec( solver_path );
			SocketChannel client = serverSocketChannel.accept();

			GameStateBuffer gameState = GameStateBuffer.newBuilder()
				.setTime( gs.getTime() )
				.setNbBarracks( number_idle_barracks )
				.setMinDistanceResourceBase( min_distance_resource_base )
				.setMaxDistanceResourceBase( max_distance_resource_base )
				.setNoInitialBase( no_initial_base_int )
				.setNoInitialBarracks( no_initial_barracks_int )
				.setResources( player.getResources() )
				.setInitialResources( initial_resources )
				.setEnemyResourcesLoss( enemy_cost_loss )
				.setWorkerMoveTime( worker_type.moveTime )
				.setWorkerHarvestTime( worker_type.harvestTime )
				.setWorkerReturnTime( worker_type.returnTime )
				.setHarvestAmount( worker_type.harvestAmount )
				.setBaseCost( base_type.cost ) 
				.setBarracksCost( barracks_type.cost )
				.setHeavyCost( heavy_type.cost )
				.setLightCost( light_type.cost )
				.setRangedCost( ranged_type.cost )
				.setMyHeavyUnits( my_heavy_units.size() )
				.setMyLightUnits( my_light_units.size() )
				.setMyRangedUnits( my_ranged_units.size() )
				.setInitialEnemyWorker( initial_number_workers )
				.setObservedEnemyWorker( observed_worker )
				.setObservedEnemyHeavy( observed_heavy )
				.setObservedEnemyLight( observed_light )
				.setObservedEnemyRanged( observed_ranged )
				.setObservedEnemyWorkerInTotal( observed_worker_in_total )
				.setObservedEnemyHeavyInTotal( observed_heavy_in_total )
				.setObservedEnemyLightInTotal( observed_light_in_total )
				.setObservedEnemyRangedInTotal( observed_ranged_in_total )
				.setSolverType( solver_type )
				.setNbSamples( nb_samples )
				.build();

			// SEND
			ByteBuffer byteBuffer = ByteBuffer.allocate( 1024 );
			byteBuffer.put( gameState.toByteArray() );
			byteBuffer.flip();
			client.write( byteBuffer );

			// RECEIVE
			ByteBuffer buf = ByteBuffer.allocate( 1024 );
			int numBytesRead = client.read( buf );

			if( numBytesRead == -1 )
			{
				client.close();
			}
			
			buf.flip();
			
			SolutionBuffer solution = SolutionBuffer.parseFrom( buf );
			number_heavy_to_produce = solution.getNumberHeavy();
			number_light_to_produce = solution.getNumberLight();
			number_ranged_to_produce = solution.getNumberRanged();
		}
		catch( IOException e1 )
		{
			System.out.println( "IO exception in process" );
			System.out.println( e1.getMessage() );
		}
		catch( NumberFormatException e3 )
		{
			no_training = true;
			number_heavy_to_produce = 0;
			number_light_to_produce = 0;
			number_ranged_to_produce = 0;
			System.out.println( "No train" );
		}
	}
	
	protected void barracksBehavior( Unit u, AtomicInteger reserved_resources )
	{
		int time = gs.getTime();
		
		//if we are on a very small map, we must play quickly
		if( map_surface <= 144 && time <= 400 && my_army.size() <= 2 && player.getResources() >= fastest_to_train_type.cost )
		{
			train( u, fastest_to_train_type );
			reserved_resources.addAndGet( fastest_to_train_type.cost );
		}
		else
			if( player.getResources() >= cheapest_type.cost && !no_training )
			{
				if( number_light_to_produce >= number_ranged_to_produce )
				{
					if( number_light_to_produce >= number_heavy_to_produce ) // number_light_to_produce higher than others
					{
						if( player.getResources() >= light_type.cost )
						{
							train( u, light_type );
							reserved_resources.addAndGet( light_type.cost );
							--number_light_to_produce;
						}
					}
					else // number_heavy_to_produce higher than others
						if( player.getResources() >= heavy_type.cost )
						{
							train( u, heavy_type );
							reserved_resources.addAndGet( heavy_type.cost );
							--number_heavy_to_produce;
						}
				}
				else // more ranged than light
					if( number_ranged_to_produce >= number_heavy_to_produce ) // number_ranged_to_produce higher than others
					{
						if( player.getResources() >= ranged_type.cost )
						{
							train( u, ranged_type );
							reserved_resources.addAndGet( ranged_type.cost );
							--number_ranged_to_produce;
						}
					}
					else // number_heavy_to_produce higher than others
						if( player.getResources() >= heavy_type.cost )
						{
							train( u, heavy_type );
							reserved_resources.addAndGet( heavy_type.cost );
							--number_heavy_to_produce;
						}
			}
	}

	protected void workersBehavior( AtomicInteger reserved_resources )
	{
		if( my_workers.isEmpty() )
			return;

		List<Unit> free_workers = new ArrayList<Unit>();

		// BASIC BEHAVIOR
		//free_workers.addAll( my_workers );
		for( Unit w : my_workers )
			if( gs.getUnitAction( w ) == null )
				free_workers.add( w );

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
		if( my_bases.isEmpty() && player.getResources() >= base_type.cost )// && !free_workers.isEmpty() )
		{
			// build a base, and don't count reserved_resources: it's top priority
			Unit u;
			if( free_workers.isEmpty() )
				u = my_workers.get(0);
			else
				u = free_workers.remove( 0 );

			AtomicInteger new_building_x = new AtomicInteger( u.getX() );
			AtomicInteger new_building_y = new AtomicInteger( u.getY() );
			spiralSearch( new_building_x, new_building_y );
			buildIfNotAlreadyBuilding( u, base_type, new_building_x.get(), new_building_y.get(), reserved_positions, player, pgs );
			reserved_resources.addAndGet( base_type.cost );
		}

		// if no barracks or plainty of money (on maps larger than 12x12 and if we have known resources around us)
		if( my_barracks.isEmpty() || ( player.getResources() >= barracks_type.cost + reserved_resources.get() + most_expensive_type.cost && map_surface > 144 && !my_resource_patches.isEmpty() ) )
		{
			// build a barracks:
			if( player.getResources() >= barracks_type.cost + reserved_resources.get() && !free_workers.isEmpty() )
			{
				// get the worker the farther away from a barracks, if any
				Unit u = free_workers.get( 0 ) ;
				if( my_barracks.isEmpty() )
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
				//System.out.println("My resource at " + r.getX() + "," + r.getY() );
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
					//System.out.println("Resource at " + r.getX() + "," + r.getY() );
				}

			// Search for resource patches
			if( closest_resource == null )
				searchResources( u );
			else
			{
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
				{
					if( pogs != null && u.getResources() == 0 )
					{
						if( pogs.observable( closest_resource.getX(), closest_resource.getY() ) )
							harvest( u, closest_resource, closest_base );
						else
							move( u, closest_resource.getX(), closest_resource.getY() );
					}
					else
						harvest( u, closest_resource, closest_base );
				}
			}
		}
	}
}
