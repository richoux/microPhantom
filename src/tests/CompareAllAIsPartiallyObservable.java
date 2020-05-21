package tests;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ContinuingAI;
import ai.core.PseudoContinuingAI;
import ai.portfolio.PortfolioAI;
import ai.*;
import ai.mcts.believestatemcts.BS3_NaiveMCTS;
import ai.microPhantom.MicroPhantom;
import ai.microPhantom.RandomMicroPhantom;
import ai.abstraction.partialobservability.POLightRush;
import ai.abstraction.partialobservability.PORangedRush;
import ai.abstraction.partialobservability.POWorkerRush;
import ai.abstraction.partialobservability.POHeavyRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.mcts.uct.DownsamplingUCT;
import ai.mcts.uct.UCT;
import ai.mcts.uct.UCTUnitActions;
import ai.minimax.ABCD.IDABCD;
import ai.minimax.RTMiniMax.IDRTMinimax;
import ai.minimax.RTMiniMax.IDRTMinimaxRandomized;
import ai.montecarlo.*;
// import ai.strategytactics.StrategyTactics;

import java.io.File;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.*;
import ai.core.InterruptibleAI;

/**
 *
 * @author santi
 */
public class CompareAllAIsPartiallyObservable {

	public static void main(String args[]) throws Exception
	{
		boolean CONTINUING = true;
		int TIME = 100;
		int MAX_ACTIONS = 100;
		int MAX_PLAYOUTS = -1;
		int PLAYOUT_TIME = 100;
		int MAX_DEPTH = 10;
		int RANDOMIZED_AB_REPEATS = 10;

		int NB_RUNS;
		int TIMEOUT;
		int NB_META_RUNS;
		String path;

		if( args.length > 0 )
		{
			NB_RUNS = Integer.parseInt( args[0] );
			path = args[1];
			TIMEOUT = Integer.parseInt( args[2] );
			NB_META_RUNS = Integer.parseInt( args[3] );
		}
		else
		{
			NB_RUNS = 1;
			path = "/home/flo/microrts_results";
			TIMEOUT = 4000;
			NB_META_RUNS = 50;
		}

		/* Timeouts in the competition
		   8x8 maps: 3000
		   16x16 maps: 4000
		   24x24 maps: 5000
		   32x32 maps: 6000
		   64x64 maps: 8000
		   > 64x64 maps: 12000
		*/

		for( int meta_run = 0; meta_run < NB_META_RUNS; ++meta_run )
		{
			List<AI> bots = new LinkedList<AI>();
			UnitTypeTable utt = new UnitTypeTable( UnitTypeTable.VERSION_ORIGINAL_FINETUNED ); // Advanced parameters
			//UnitTypeTable utt = new UnitTypeTable( UnitTypeTable.VERSION_NON_DETERMINISTIC );

			System.out.println("Base cost: " + utt.getUnitType( "Base" ).cost);
			
			// microRTS competition public maps
			//PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8A.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16A.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/BWDistantResources32x32.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/BroodWar/(4)BloodBath.scmB.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/FourBasesWorkers8x8.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/TwoBasesBarracks16x16.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/NoWhereToRun9x8.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/DoubleGame24x24.xml", utt);

			// microRTS competition hidden maps
			// 2019
			//PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/noBases8x8.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/GardenOfWar64x64.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/melee14x12Mixed18.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/barricades24x24.xml", utt);
			// 2018
			//PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16noResources.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/itsNotSafe.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/letMeOut.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/chambers32x32.xml", utt);

			// not in the competition
			PhysicalGameState pgs = PhysicalGameState.load("maps/12x12/basesWorkers12x12A.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/24x24/basesWorkers24x24A.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/32x32/basesWorkers32x32A.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/64x64/basesWorkers64x64A.xml", utt);
			//PhysicalGameState pgs = PhysicalGameState.load("maps/BroodWar/(2)Benzene.scxA.xml", utt);

			GameState gs = new GameState( pgs, utt );

			//bots.add( new MicroPhantom( utt, "src/ai/microPhantom/solver_cpp" ) );
			bots.add(new RandomMicroPhantom(utt, "src/ai/microPhantom/solver_cpp"));

			// bots.add(new StrategyTactics(utt));
			// bots.add(new RandomAI(utt));
			// bots.add(new RandomBiasedAI());
			// bots.add(new POLightRush(utt, new BFSPathFinding()));
			// bots.add(new PORangedRush(utt, new BFSPathFinding()));
			// bots.add(new POWorkerRush(utt, new BFSPathFinding()));
			// bots.add(new POHeavyRush(utt, new BFSPathFinding()));
			bots.add( new POLightRush( utt ) );
			//bots.add(new PORangedRush(utt));
			//bots.add(new POWorkerRush(utt));
			// bots.add(new POHeavyRush(utt));
			// bots.add(new BS3_NaiveMCTS(utt));

			// public BS3_NaiveMCTS(int available_time, int max_playouts, int lookahead, int max_depth,
			//         float e_l, float e_g, float e_0, AI policy, EvaluationFunction a_ef, boolean fensa) {
			//     super(available_time, max_playouts, lookahead, max_depth, e_l, e_g, e_0, policy, a_ef, fensa);
			// }


			// bots.add(new PortfolioAI(new AI[]{new WorkerRush(utt, new BFSPathFinding()),
			//                                   new LightRush(utt, new BFSPathFinding()),
			//                                   new RangedRush(utt, new BFSPathFinding()),
			//                                   new RandomBiasedAI()},
			//                          new boolean[]{true,true,true,false},
			//                          TIME, MAX_PLAYOUTS, PLAYOUT_TIME*4, new SimpleSqrtEvaluationFunction3()));

			// bots.add(new IDRTMinimax(TIME, new SimpleSqrtEvaluationFunction3()));
			// bots.add(new IDRTMinimaxRandomized(TIME, RANDOMIZED_AB_REPEATS, new SimpleSqrtEvaluationFunction3()));
			// bots.add(new IDABCD(TIME, MAX_PLAYOUTS, new LightRush(utt, new GreedyPathFinding()), PLAYOUT_TIME, new SimpleSqrtEvaluationFunction3(), false));

			// bots.add(new MonteCarlo(TIME, PLAYOUT_TIME, MAX_PLAYOUTS, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
			// bots.add(new MonteCarlo(TIME, PLAYOUT_TIME, MAX_PLAYOUTS, MAX_ACTIONS, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
			// // by setting "MAX_DEPTH = 1" in the next two bots, this effectively makes them Monte Carlo search, instead of Monte Carlo Tree Search
			// bots.add(new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, 1, 0.33f, 0.0f, 0.75f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true));
			// bots.add(new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, 1, 1.00f, 0.0f, 0.25f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true));

			// bots.add(new UCT(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
			// bots.add(new DownsamplingUCT(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_ACTIONS, MAX_DEPTH, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
			// bots.add(new UCTUnitActions(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH*10, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3()));
			// bots.add(new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, 0.33f, 0.0f, 0.75f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true));
			// bots.add(new NaiveMCTS(TIME, MAX_PLAYOUTS, PLAYOUT_TIME, MAX_DEPTH, 1.00f, 0.0f, 0.25f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true));

			if( CONTINUING )
			{
				// Find out which of the bots can be used in "continuing" mode:

				List<AI> bots2 = new LinkedList<>();
				for( AI bot : bots )
				{
					if( bot instanceof BS3_NaiveMCTS )
						bot.preGameAnalysis( gs, 100 );
					if( bot instanceof AIWithComputationBudget )
					{
						if( bot instanceof InterruptibleAI )
							bots2.add( new ContinuingAI( bot ) );
						else 
							bots2.add( new PseudoContinuingAI( (AIWithComputationBudget)bot ) );
					}
				}
			
				bots = bots2;
			}

			
			PrintStream out1, out2;
			if( NB_META_RUNS > 1 )
			{
				out1 = new PrintStream( new File( path + "_up_" + meta_run + ".txt" ) );
				out2 = new PrintStream( new File( path + "_down_" + meta_run + ".txt" ) );
			}
			else
			{
				out1 = new PrintStream( new File( path + "_up.txt" ) );
				out2 = new PrintStream( new File( path + "_down.txt" ) );
			}

			// Separate the matchs by map:
			List<PhysicalGameState> maps = new LinkedList<PhysicalGameState>();
			maps.clear();
			maps.add( pgs );

			// in a modified src/test/Experimenter.java
			// void runExperiments( List<AI> bots, List<PhysicalGameState> maps, UnitTypeTable utt, int iterations, int max_cycles, int max_inactive_cycles, boolean visualize,
			//                      PrintStream out1, PrintStream out2, int run_only_those_involving_this_AI, boolean skip_self_play, boolean partiallyObservable,
			//                      boolean saveTrace, boolean saveZip, String traceDir )
			Experimenter.runExperiments( bots, maps, utt, NB_RUNS, TIMEOUT, 300, false,
			                             out1, out2, 0, true, true,
			                             false, false, "" );
		}
	}
}

