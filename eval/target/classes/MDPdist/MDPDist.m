(* ::Package:: *)

BeginPackage["MDPDist`"]


MDP::usage="MDP[<transition matrix>, <label vector>, <inputs>, <output>] is the data structure for an Markov Decision process with I/O.";


CMDP::usage="MDP[<encoding maps>,<MDP list>] is the data structure for the (asynchronous) parallel composition of a list of MDPs";


RandomMDP::usage="RandomMDP[sizeS,sizeA,maxDegree] returns an pseudo-random MDP with sizeS states, and sizeA actions."


getEncodingMaps::usage="getEncodingMaps[<MDP list>] returns the encoding maps for the parallel composition of a list of MDPs";


pairFromComponents::usage="pairFromComponents[<encoding maps>][<components pair>] returns the pair corresponding to the given components pair."


ParallelComposition::usage="ParallelComposition[<CMDP>] returns the MDP data structure corresponding to the given CMDP";


ParallelMDPs::usage="ParallelMDPs[<MDP list>] returns the CMDP data structure corresponding to the (asynchronous) parallel composition of a list of MDPs";


MDPQ::usage="MDPQ[<mdp>] checks if the give MDP is well-defined";


MDPrw::usage="MDPrw[<array rules>, n, m] returns an n\[Times]m reward matrix.";


MDPtm::usage="MDPtm[<array rules>, n, m] returns an n\[Times]m\[Times]n transition matrix.";


JoinMDP::usage="JoinMDP[<mdp>,...,<mdp>] yields an MDP representing the disjoint union of the given sequence of MDPs";


PlotMDP::usage="PlotMDP[<mdp>] displays the Markov decision process.";


BDistMDP::usage="BDistMDP[<mdp>, <discount factor>, <query list>] computes the bisimilarity distance between all the pairs of states in <query list>.";


Verbose::usage="Option for BDistMDP (default value False). When the Verbose option is True a trace of the computation is displayed";


Exact::usage="Option for BDistMDP (default value False).  When the Exact option is True the result is exact";


ConsistencyCheck::usage="Option for BDistMDP (default value True). When the ConsistencyCheck is set True, a sanity check of the input is performed before starting the compuatation.";


Estimates::usage="Option for BDistMDP (default value None). An estimate for the pair {i,j} can be given as {i,j} -> e_ij";


BisimQuotientMDP::usage="BisimQuotientMDP[<mdp>] yields and MDP representing the bisimilarity quotient of the given MDP";


BisimClassesMDP::usage="BisimClassesMDP[<mdp>] return the set of states of the given MDP partitioned into bisimilarity classes";


Begin["`Private`"]


(* ::Section:: *)
(*Implementation*)


(* ::Subsection::Closed:: *)
(*LP Utilities*)


GetConstrMatrix[inequalities_, vars_]:=
Module[{ConstrRow,rows,consts},
ConstrRow[inequality_]:=
Module[{b,A},
{b,A}=CoefficientArrays[Equal@@inequality,vars];
{A,{-b,Head[inequality]/.{LessEqual->-1,Equal->0,GreaterEqual->1}}}
];
ArrayFlatten/@Thread[ConstrRow/@inequalities]
];


(* ::Subsection::Closed:: *)
(*Utilities*)


SetAttributes[SequentialTest,HoldRest];
SequentialTest[]=True;
SequentialTest[guard_,act_,rest___]:= 
	If[guard,SequentialTest[rest],ReleaseHold[act]];


DebugPrint[s___]:=Print[s]/;OptionValue[BDistMDP,Verbose];


PrPrint[IndexedSchedule[mapS_,mapT_,basicCells_,V_]]:=
Module[{basic},
basic[value_,index_]:=Style[EpsilonRemoval@value,Red,Bold]/;MemberQ[basicCells,index];
basic[value_,_]:=value;
TableForm[MapIndexed[basic,V,{2}],TableHeadings->{ToString[#[[2]]]&/@mapS,ToString[#[[2]]]&/@mapT}]
];
PrPrint[x_]:=x;


PrintActiveProblems[dist_,coupling_,queryLst_]:=
Module[{ActivePrint,active = ReachableProblems[dist,coupling,queryLst]},

ActivePrint[{u_,v_}->actv_]:=
Print[{u,v}->TableForm[PrPrint/@Normal[coupling[[u,v]]],TableDirections->Row]]/;actv;
Print["Reachable states from ",queryLst, " in the current coupling"];
Scan[ActivePrint,ArrayRules[active]]
]/;OptionValue[BDistMDP,Verbose];


(* ::Subsection:: *)
(*Markov Decision Processes (MDPs) with I/O*)


(* ::Text:: *)
(* mdpDimensions[M] returns the pair {|S|, |A|} where S is the set of states and A the set of actions*)


mdpDimensions[mdp:MDP[\[Tau]_,lbl_,aLbl_]]:= {Length[lbl],Length[mdpActions@mdp]};
mdpDimensions[mdp:CMDP[enc_,mdpLst_]]:={Length[enc[[2]]],Length[mdpActions@mdp]};


MDPtm[rules_, n_, m_]:= SparseArray[rules,{n,m,n},0];


MDPrw[rules_, n_, m_]:= SparseArray[rules,{n,m},0];


mdpActions[MDP[\[Tau]_,lbl_,aLbl_]]:=aLbl;
mdpActions[CMDP[enc_,mdpLst_]]:=Union@@(mdpActions/@mdpLst);


reward[MDP[\[Tau]_,\[Rho]_,aLbl_]][s_,al_]:=
Module[{act=Position[aLbl,al]},If[act=={},0,\[Rho][[s,act[[1,1]]]]]];
reward[CMDP[enc_,mdpLst_]][s_,al_]:=
Module[{g},
g[t_,mdp_]:=reward[mdp][t,al];
Plus@@MapThread[g,{stateToComponents[enc][s],mdpLst}]
];


label[MDP[\[Tau]_,lbl_,aLbl_]][s_]:=lbl[[s]];


getDistribution[mdp:MDP[\[Tau]_,lbl_,aLbl_],s_,al_]:=
Module[{sizeS=mdpDimensions[mdp][[1]],a},
a=Position[aLbl,al][[1,1]];
Cases[Thread[Rule[Table[i,{i,sizeS}],\[Tau][[s,a,All]]]],Except[_->0]]
]/; MemberQ[aLbl,al];
getDistribution[mdp:MDP[\[Tau]_,lbl_,aLbl_],s_,al_]:={s->1};

getDistribution[CMDP[enc_,mdpLst_],s_,al_]:=
Module[{g,distributions,F},
g[t_,mdp_]:=getDistribution[mdp,t,al];
distributions=MapThread[g,{stateToComponents[enc][s],mdpLst}];
F[x___]:=stateFromSubStates[enc][List[x][[All,1]]]-> Times@@(List[x][[All,2]]);
Flatten@Outer[F,Sequence@@distributions]
];


ParallelComposition[cmdp:CMDP[{map_,revmap_},mdpLst_]]:=
Module[{sizeA,sizeS,actions,states,amap,tau,\[Tau],\[Rho]},
actions=mdpActions[cmdp];states=revmap[[All,1]];
sizeA=Length@actions; sizeS=Length@states;
amap=Thread[Rule[actions,Table[i,{i,Length@actions}]]];
tau[s_,al_]:={s,al/.amap,#[[1]]}->#[[2]]&/@getDistribution[cmdp,s,al];
\[Tau]=SparseArray[Flatten@Table[tau[s,al],{s,states},{al,actions}],{sizeS,sizeA,sizeS},0];
\[Rho]=SparseArray[Flatten@Table[{s,al/.amap}->reward[cmdp][s,al],{s,states},{al,actions}],{sizeS,sizeA},0];
MDP[\[Tau],\[Rho],actions]
];


RationalArrayQ[m_,depth_]:= ArrayQ[m,depth,MatchQ[#,_Rational|_Integer]&];
RealArrayQ[m_,depth_]:= ArrayQ[m,depth,MatchQ[#,_Real|_Integer]&];


MDPQ::tm="`1` must be a `2` transition matrix";
MDPQ::tmdim="`1` should have dimension `2` but has `3`";
MDP::tmdistr="`1` is not a probabilistic transition matrix";
MDPQ::rw="`1` must be a `2` reward matrix";
MDPQ::acttype="`1` must be either a list of strings or a list of integers.";
MDPQ::actdupl="`1` has some duplicate actions.";
MDPQ::rwact="The number of actions is `1` but the reward matrix is defined for `2` different actions.";
MDPQ::lblsize="The label vector `1` must be of length `2`.";


MDPQ[rationalQ_][mdp:MDP[\[Tau]_,L_,aLbl_]]:=
Module[{sizeA,sizeS,matrixtype,rationalLbl},
matrixtype:=If[rationalQ,RationalArrayQ,RealArrayQ];
rationalLbl:=If[rationalQ,"rational","real"];
If[SequentialTest[
matrixtype[\[Tau],3],(Message[MDPQ::tm,\[Tau],rationalLbl];False),
MatchQ[L,{__Integer}|{___String}],(Message[MDPQ::acttype,L];False),
MatchQ[aLbl,{__Integer}|{___String}],(Message[MDPQ::acttype,aLbl];False)],
(* typecheck successully passed. checking for data inconsistency *)
{sizeS,sizeA}=mdpDimensions[mdp];
SequentialTest[
Length[DeleteDuplicates[aLbl]]==Length[aLbl],(Message[MDPQ::actdupl,aLbl];False),
Length[L]==sizeS,(Message[MDPQ::lblsize,L,sizeS];False),
Length[aLbl]==sizeA,(Message[MDPQ::rwact,Length[aLbl],sizeA];False),
Dimensions[\[Tau]]=={sizeS,sizeA,sizeS},(Message[MDPQ::tmdim,MatrixForm[\[Tau]],{sizeS,sizeA,sizeS},Dimensions[\[Tau]]];False),
And@@(Flatten[Map[Total[#]==1&,\[Tau],{2}]]),(Message[MDP::tmdistr,MatrixForm[\[Tau]]];False)],
(* data consistency failed *)
False
]];

MDPQ[rationalQ_][CMDP[{map_,revmap_},mdpLst_List]]:=And@@(MDPQ[rationalQ]/@mdpLst);

MDPQ[_][_]:=False;


(* ::Program:: *)
(*MDPQ[rationalQ_][MDP[\[Tau]_, \[Rho]_, aLbl_]] :=*)
(*  Module[{sizeA, sizeS},*)
(*    {sizeS, sizeA} = Dimensions[\[Rho]];*)
(*    (* check if the dimensions are consistent, and if \[Tau] is a probability distribution matrix *)*)
(*    Length[aLbl] == sizeA \[And] Dimensions[\[Tau]] == {sizeS, sizeA, sizeS} \[And] And @@ (Flatten[Map[Total[#] == 1 &, \[Tau], {2}]])*)
(*    ] /; MatchQ[aLbl, {__Integer} | {___String}] && (* aLbl is either a list of strings or a list integers *)*)
(*    If[rationalQ, RationalArrayQ[\[Tau], 3] && RationalArrayQ[\[Rho], 2], RealArrayQ[\[Tau], 3] && RealArrayQ[\[Rho], 2]];*)
(**)
(*MDPQ[rationalQ_][CMDP[{map_, revmap_}, mdpLst_List]] := And @@ (MDPQ[rationalQ] /@ mdpLst);*)
(**)
(*MDPQ[_][_] := False;*)


(* ::Text:: *)
(*PlotMDP[M] returns the decision graph induced by M*)


PlotMDP[mdp:MDP[\[Tau]_,L_,aLbl_]]:=
Module[{sizeS,sizeA,fromActionEdge,vrf,erf,toActionEdges,fromActionEdges,radius=0.05},
fromActionEdge[0,_]:={};
fromActionEdge[val_,{s_,a_,t_}]:=Labeled[DirectedEdge[{s,a},t],val];
(* vertex rendering function *)
vrf[pos_,{s_,a_},{w_,h_}]:={Hue[a/sizeA,1,1,0.3],EdgeForm[Black],Disk[pos,radius],Opacity[1],Black, Text[aLbl[[a]],pos]};
vrf[pos_,s_,{w_,h_}]:={White,EdgeForm[Black],Disk[pos,.1],Black,Text[{s,L[[s]]},pos]};
(* edge rendering function *)
erf[points_,lbl_]:={Black,Arrowheads[0.02],Arrow[BezierCurve[points],{radius,0.1}],
Inset[N[lbl[[2]],2],Mean@points,Background->White]};

{sizeS,sizeA}=mdpDimensions@mdp;
toActionEdges=Flatten@Table[s->{s,a},{s,sizeS},{a,sizeA}];
fromActionEdges=List@@#&/@Flatten@MapIndexed[fromActionEdge,\[Tau],{3}];
GraphPlot[Join[toActionEdges,fromActionEdges],VertexShapeFunction->vrf]
];
PlotMDP[CMDP[enc_,mdpLst_]]:=PlotMDP/@mdpLst;


JoinMDP[mdps___]:=Fold[JoinMDPAux,First@{mdps},Rest@{mdps}];
JoinMDPAux[mdp1:MDP[\[Tau]1_,\[Rho]1_,act1_],mdp2:MDP[\[Tau]2_,\[Rho]2_,act2_]]:=
Module[{s1,s2,sloop,nrew,act,shiftrule,noact1,noact2},

sloop[S_,noact_] := Flatten@Table[{s,a,s}->1, {s,S},{a,noact}];
nrew[S_,mdp_] := Flatten@Table[{s,a}-> reward[mdp][s,act[[a]]],{s,S},{a,Length[act]}];

s1=mdpDimensions[mdp1][[1]]; s2=mdpDimensions[mdp2][[1]];
act=Union[mdpActions@mdp1,mdpActions@mdp2]; (* all actions *)
noact1=Flatten[Position[act,Alternatives@@Complement[act2,act1]]];
noact2=Flatten[Position[act,Alternatives@@Complement[act1,act2]]];

shiftrule[offset_][{i_,k_}->x_]:={i+offset,k}->x;
shiftrule[offset_][{i_,k_,j_}->x_]:={i+offset,k,j+offset}->x;

\[Tau] = SparseArray[Join[
Most[ArrayRules[\[Tau]1]],sloop[s1,noact1],
shiftrule[s1]/@Most[ArrayRules[\[Tau]2]],shiftrule[s1]/@sloop[s2,noact2]],
{s1+s2,Length[act],s1+s2}];

\[Rho]= SparseArray[Join[
Most[ArrayRules[\[Rho]1]],nrew[s1,mdp1],
shiftrule[s1]/@Most[ArrayRules[\[Rho]2]],shiftrule[s1]/@nrew[s2,mdp2]],
{s1+s2,Length@act}];

MDP[\[Tau],\[Rho],act]
];




(* ::Text:: *)
(*AllPairs[M] returns all states pairs of the given MDP*)


AllPairs[mdp_MDP]:=
Module[{sizeS=mdpDimensions[mdp][[1]]},
Join@@(Table[{i,j},{i,sizeS},{j,sizeS}])
];
AllPairs[CMDP[{map_,revmap_},mdpLst_]]:=
Join@@(Table[{i,j},{i,map[[All,1]]},{j,map[[All,1]]}]);


IncompatiblePairs[mdp:MDP[\[Tau]_,L_,aLbl_]]:=Select[AllPairs[mdp],Not@MatchQ[L[[#[[1]]]],L[[#[[2]]]]]&];


(* ::Subsection::Closed:: *)
(*Random generation of MDPs*)


RandomRational[rmax_,n_]:=(RandomInteger[{1,#}]/#)&/@RandomInteger[{1,rmax},n];


RandomProbDistr[rmax_,n_]:=
Module[{rnd=RandomRational[rmax,n],total},
total=Total[rnd]; (#/total)&/@rnd ];


RandomRewardMatrix[numStates_,numActions_,interval:{minrew_,maxrew_}]:=
SparseArray[Rationalize[RandomReal[interval,{numStates,numActions}],0]];


RandomTransitionMatrix[numStates_,numActions_,maxDegree_]:=
Module[{stateDistr},
stateDistr[degree_]:=Shuffle[Join[Table[0,{numStates-degree}],RandomProbDistr[numStates,degree]]];
SparseArray[Table[stateDistr[RandomInteger[{1,maxDegree}]],{numStates},{numActions}]]
]/;numStates>= maxDegree;
RandomTransitionMatrix[numStates_,maxDegree_]:=
(Message[RandomTransitionMatrix::nnarg,numStates,maxDegree];Abort[]);


RandomTransitionMatrix::nnarg="The argument `1` is not greater than or equal to `2`.";


RandomMDP[numStates_,numActions_,maxDegree_]:=
MDP[RandomTransitionMatrix[numStates,numActions,maxDegree],
RandomRewardMatrix[numStates,numActions,{0,1}],Table[i,{i,numActions}]];


Shuffle[{}]:={};
Shuffle[lst_]:=
Module[{left,right,pick=RandomInteger[{1,Length@lst}]},
left=Take[lst,pick];right=Drop[lst,pick];
Join[{lst[[pick]]},Shuffle[Most@left],Shuffle[right]]
];


(* ::Subsection::Closed:: *)
(*Transportation Problem*)


(* ::Text:: *)
(*Given two \[Epsilon]-numbers x and y, EpsilonLeq[x,y] determines if x <= y provided that \[Epsilon] -> +0.*)


EpsilonLeq[x_,y_]:=
If[EpsilonRemoval[x-y]==0,(* check if x-y depends only on \[Epsilon]*)
EpsilonFix[x-y,1]<= 0,(* set \[Epsilon] > 0 and check again *)
EpsilonRemoval[x-y]<= 0 (* set e[]= 0 and check again *)
];


EpsilonMin[x_,y_]:= If[EpsilonLeq[x,y],x,y];


EpsilonPerturbation[\[Pi]s_,\[Pi]t_]:=
Module[{AddEpsilon},
AddEpsilon[k_][{a_,id_}]:={a + k e[],id};
{AddEpsilon[1]/@\[Pi]s, Append[Most[\[Pi]t],AddEpsilon[Length@\[Pi]s][Last@\[Pi]t]]}
];


EpsilonFix[x_,\[Epsilon]value_]:=x/.e[]->\[Epsilon]value;


EpsilonRemoval[x_]:=EpsilonFix[x,0];


indexed[lst_]:=Thread[{lst,Table[i,{i,1,Length@lst}]}];


GuessVertexAux[_][\[Pi]s_,\[Pi]t_]:={}/;\[Pi]s=={}||\[Pi]t=={};
GuessVertexAux[strategy_][\[Pi]s_,\[Pi]t_]:=
Module[{G},
G[{i_,j_}]:=
Module[{rec},
rec=If[EpsilonLeq[\[Pi]s[[i,1]],\[Pi]t[[j,1]]],(*\[Pi]s\[LeftDoubleBracket]i,1\[RightDoubleBracket] \[LessEqual] \[Pi]t\[LeftDoubleBracket]j,1\[RightDoubleBracket]*)
GuessVertexAux[strategy][Drop[\[Pi]s,{i}],ReplacePart[\[Pi]t,{j,1}->\[Pi]t[[j,1]]- \[Pi]s[[i,1]]]],
GuessVertexAux[strategy][ReplacePart[\[Pi]s,{i,1}->\[Pi]s[[i,1]]- \[Pi]t[[j,1]]],Drop[\[Pi]t,{j}]]];
Prepend[rec,{\[Pi]s[[i,2]],\[Pi]t[[j,2]]}->EpsilonMin[\[Pi]s[[i,1]],\[Pi]t[[j,1]]]]
];
G[strategy[\[Pi]s,\[Pi]t]]
];


NorthWestStrategy[_,_]:={1,1};


ToMatrix[\[Pi]s_,\[Pi]t_,vertex_]:=
Module[{mapS,revmapS,mapT,revmapT,toMap,mapIndex,mappedvertex},
toMap[indexlst_]:=
Module[{index=Table[i,{i,1,Length@indexlst}] },
{Thread[Rule[index,indexlst]],Thread[Rule[indexlst,index]]}];
mapIndex[{s_,t_}->val_]:={s/.revmapS,t/.revmapT}->val;

{{mapS,revmapS},{mapT,revmapT}} = toMap[#[[All,2]]]&/@{\[Pi]s,\[Pi]t};
mappedvertex= mapIndex/@vertex;
IndexedSchedule[mapS,mapT,mappedvertex[[All,1]],SparseArray[mappedvertex,Length/@{\[Pi]s,\[Pi]t}]]
];


GuessVertex[\[Pi]s_,\[Pi]t_]:=
Module[{\[Epsilon]\[Pi]s,\[Epsilon]\[Pi]t},
{\[Epsilon]\[Pi]s,\[Epsilon]\[Pi]t}=EpsilonPerturbation[\[Pi]s, \[Pi]t];
ToMatrix[\[Epsilon]\[Pi]s,\[Epsilon]\[Pi]t,GuessVertexAux[NorthWestStrategy][\[Epsilon]\[Pi]s,\[Epsilon]\[Pi]t]]
];


(* ::Text:: *)
(*AddtoBase[d, vertex] == {reducedcost, non-basic cell} *)
(*returns the non-basic cell which has the minimal reduced cost, if the vertex has no non-basic cells, then it returns {\[Infinity], {0,0}}.*)


AddtoBase[costMatrix_,sh:IndexedSchedule[mapS_,mapT_,basicCells_,V_]]:=
Module[{sizeS=Length@mapS, sizeT=Length@mapT, 
t,getCost,ReducedCost,uv,makeRow,base},

makeRow[{1,j_}]:={SparseArray[{{1,sizeS+j}->1},{1,sizeS+sizeT}]};
makeRow[{i_,j_}]:={SparseArray[{{1,i}->1,{1,sizeS+j}->1},{1,sizeS+sizeT}]};

getCost[{i_,j_}]:=costMatrix[[i,j]];

ReducedCost[_,{i_,j_}]:=
Module[{reducedcost=Chop[getCost[{i,j}]-uv[[i]]-uv[[sizeS+j]],10^-5]},
If[base[[1]]>reducedcost,
base={reducedcost,{i,j}} (* update the current least reduced cost *)
];reducedcost];


base={\[Infinity],{0,0}};(* init cell *)
If[sizeS != 1 || sizeT != 1,
(* u-v modifiers (Subscript[u, 1],...,Subscript[u, sizeS],Subscript[v, 1],...,Subscript[v, sizeT]) *)
uv=Module[{A,b},
A=Normal@ArrayFlatten[makeRow/@basicCells];
b=getCost/@basicCells;
(*Print["u-v Problem:",MatrixForm@ArrayFlatten[makeRow/@basicCells],
MatrixForm@uv,MatrixForm[getCost/@basicCells]];*)
Check[LinearSolve[A,b],Print[A];Print[b];Print[costMatrix];Print[sh];Abort[]]
];
(* scan the cells computing their reduced cost *)
MapIndexed[ReducedCost,V,{2}];
];
base (* return the cell with the least reduced cost *)
];


(* ::Text:: *)
(*SteppingStone[non-basic cell, vertex] returns the vertex obtained by inserting the given non-basic cell into the base *)


SteppingStone[{s_,t_},vertex:IndexedSchedule[mapS_,mapT_,basicCells_,V_]]:=
Module[{M,d,oldBasicCell,newBasicCells},
{oldBasicCell,M}=SteppingStonePath[{s,t},vertex];
(*DebugPrint[MatrixForm@M];*)
(*DebugPrint["old basic cell: ",oldBasicCell, ", new basic cell: ",{s,t}];*)
d= Extract[V,oldBasicCell];
(*DebugPrint["ammount moved: ",d];*)
newBasicCells = basicCells/.oldBasicCell->{s,t};
(*DebugPrint["Old basic cells:",basicCells,", new basic cells",newBasicCells];*)
IndexedSchedule[mapS,mapT,newBasicCells,SparseArray[V+d M]]
];


SteppingStonePath[{s_,t_},vertex:IndexedSchedule[mapS_,mapT_,basicCells_,V_]]:=
Module[{neighbors,visit,parent,cell,path,getMatrix,visited,f},

visited[_]:=False;
neighbors[{u_,True}]:={#[[2]],False}&/@(Select[basicCells,#[[1]]==u \[And] Not[visited[{#[[2]],False}]]&]);
neighbors[{v_,False}]:={#[[1]],True}&/@(Select[basicCells,#[[2]]==v \[And] Not[visited[{#[[1]],True}]]&]);

f[{{a_,True},{b_,False}}]:={a,b};
f[{{a_,False},{b_,True}}]:={b,a};

visit[node_]:=
(visited[node]=True;
Scan[(parent[#]=node; visit@#)&,neighbors[node]]
);

getMatrix[{},sign_]:={};
getMatrix[lst_,sign_]:=
(If[sign<0\[And] EpsilonLeq[Extract[V,First@lst] ,Extract[V,cell]],cell=First@lst];(* update cell *)
Append[getMatrix[Rest@lst,- sign], First@lst -> sign]); (* compute arrayrules *)

(*DebugPrint[PrPrint@vertex];*)

(* search for an alternating path from s to t *)
parent[{s,True}]={};(* {s,True} is the root of the tree *)
visit[{s,True}]; (* compute the spanning tree rooted in {s,True} *)

(* get the alternating path from {t,False} to {s,True} *)
path=Append[f/@Partition[(Most@NestWhileList[parent,{t,False},#!={}&]),2,1],{s,t}];
cell=First@path; path=getMatrix[path,-1];

{cell,SparseArray[path,Dimensions@V]}
];


BestVertex[costMatrix_,{redCost_,nbCell_},vertex_]:=
Module[{currVertex=vertex, currRedCost=redCost,currNbCell= nbCell},
While[currRedCost<0,
currVertex= SteppingStone[currNbCell,currVertex];(* move to a better vertex *)
(*DebugPrint[PrPrint@currVertex];*)
(* update the current least reduced cost *)
{currRedCost,currNbCell}= AddtoBase[costMatrix,currVertex];
];
currVertex
];


(* ::Subsection:: *)
(*On-the-fly Algorithm for computing Bisimilarity Pseudometric*)


FixedDistanceQ[dist_,pair:{_,_}]:=MatchQ[Extract[dist,Sort@pair],_Fixed] ;(* says if the distance is fixed or not *)


VisitedPairQ[dist_,coupling_,{s_,t_}]:= (* decide whether the distance pair has already been considered *)
FixedDistanceQ[dist,{s,t}]\[Or]And@@(MatchQ[#,_IndexedSchedule | _CMatching]&/@Normal[coupling[[s,t]]]);
(*MatchQ[Normal[coupling[[s,t]]],{___IndexedSchedule}];*)


NextDemandedPairs[IndexedSchedule[mapS_,mapT_,basicCells_,V_]]:= 
DeleteDuplicates[(Sort[{#[[1]]/.mapS,#[[2]]/.mapT}]&)/@Select[basicCells,EpsilonRemoval[Extract[V,#]]>0&]];
NextDemandedPairs[CMatching[rulesLst_]]:=DeleteDuplicates[Sort/@(rulesLst[[All,1]])];
NextDemandedPairs[transitions_List]:=
DeleteDuplicates[Join@@(NextDemandedPairs/@transitions)];


AddPair[MDP[_,_,_],dist_][coupling_,pair:{_,_}]:= coupling/; VisitedPairQ[dist,coupling,pair];
AddPair[mdp:MDP[\[Tau]_,_,_],dist_][coupling_,pair:{s_,t_}]:=
Module[{Marginal,newtransitions,sizeA},
sizeA=mdpDimensions[mdp][[2]];
Marginal[state_,a_]:=Select[indexed[\[Tau][[state,a,All]]],#[[1]]>0&];
newtransitions := (* guess a fesible TP schedule for each action *)
Table[GuessVertex[Marginal[s,a],Marginal[t,a]],{a,sizeA}];
(* recursively update the coupling *)
UpdateCoupling[mdp,dist][coupling,pair,newtransitions]
];


AddPairs[mdp:MDP[_,_,_],dist_][coupling_,queryLst_]:=Fold[AddPair[mdp,dist],coupling,queryLst];


UpdateCoupling[mdp:MDP[_,_,_],dist_][coupling_,{s_,t_},transitions:{___IndexedSchedule}]:=
AddPairs[mdp,dist][ReplacePart[coupling,{s,t}->transitions],NextDemandedPairs[transitions]];
UpdateCoupling[mdp:MDP[_,_,_],dist_][coupling_,{s_,t_,a_},schedule_IndexedSchedule]:=
AddPairs[mdp,dist][ReplacePart[coupling,{s,t,a}->schedule],NextDemandedPairs[schedule]];


ReachableProblems[dist_,coupling_,queryLst_]:=
Fold[DFS[dist,coupling],SparseArray[{},Dimensions@dist,False],queryLst];
DFS[dist_,coupling_][visited_,{s_,t_}]:=visited/;visited[[s,t]]\[Or]FixedDistanceQ[dist,{s,t}];
DFS[dist_,coupling_][visited_,{s_,t_}]:=
Fold[DFS[dist,coupling],ReplacePart[visited,{s,t}->True],NextDemandedPairs[Normal[coupling[[s,t]]]]];


ActivePairs[dist_,coupling_,queryLst_]:=
Most[ArrayRules[ReachableProblems[dist,coupling,queryLst]]][[All,1]];


(* list of active pairs activeLst=ActivePairs[dist,coupling,queryLst];*)
UpdateOptPolicyValue[\[Lambda]_,mdp_,dist_,coupling_,activeLst_]:=
Module[{f,g,h,optAction,
xVars,xVals,pVars,pVals,
policy,updatedDist,sizeS,sizeA,actions,\[Rho]},

\[Rho][s_,a_]:=reward[mdp][s,actions[[a]]];

(* Bellman-optimality equation's constraints *)
f[{s_,t_}]:=Flatten@Table[{
	x[s,t]>= p[s,t,a], 
	p[s,t,a]== \[Lambda] g[coupling[[s,t,a]]]
},{a,sizeA}];

(* Optimal equation *)
h[optAct_][{s_,t_}]:= Module[{a=optAct[[s,t]]},
x[s,t]== \[Lambda] g[coupling[[s,t,a]]]
];

g[CMatching[trRules_]]:=
Module[{var},
var[pair_-> val_]:=getDistValue[dist,pair]/;FixedDistanceQ[dist,pair];
var[pair_-> val_]:=x@@Sort[pair]*val;
Plus@@(var/@trRules)];
g[IndexedSchedule[mapS_,mapT_,basicCells_,V_]]:=
Module[{var},
var[{i_,j_}]:=getDistValue[dist,{i/.mapS,j/.mapT}]/;FixedDistanceQ[dist,{i/.mapS,j/.mapT}];
var[{i_,j_}]:=x@@Sort[{i/.mapS,j/.mapT}];
Plus@@(var[#]*EpsilonRemoval[Extract[V,#]]&/@basicCells)];
optAction[x_,lst_]:=Ordering[Abs[x-#]&/@lst][[1]];(*Position[lst,x]\[LeftDoubleBracket]1,1\[RightDoubleBracket];*)

actions=mdpActions[mdp];
{sizeS,sizeA}=mdpDimensions[mdp];

(* set the LP for solving the value of the active part of the coupling *)
xVars=x@@#&/@activeLst;(* value variables *)
pVars=Flatten[Table[p@@Append[#,a],{a,sizeA}]&/@activeLst];(* policy variables *)
{xVals,pVals}= Module[{vars=Join[xVars,pVars],A,b,c,sol},
{A,b}=GetConstrMatrix[Flatten[f/@activeLst],vars];
(* solve the value problem for the current coupling  *)
(*Print["Solving Bellman Equation"];*)
c = CoefficientArrays[Plus@@xVars,vars][[2]];
sol = Quiet[LinearProgramming[c,A,b,Method->"InteriorPoint"],{LinearProgramming::lpipp}];(*"RevisedSimplex"*)
(*Print["Solved"];*)
{Take[sol,Length@activeLst],(* x Values *)
Partition[Take[sol,-Length@pVars],sizeA]} (* p\[LeftDoubleBracket]a\[RightDoubleBracket] values for each action *)
];

(* get the optimal policy *)
policy=SparseArray[
Thread[Rule[activeLst,MapThread[optAction,{xVals,pVals}]]],
{sizeS,sizeS}];
(* compute exact values *)

If[OptionValue[BDistMDP,Exact], (* check if the solution is required to be exact *)
Module[{A,b}, (* solve exactly the linear equation system induced by the optimal policy *)
{b,A}=CoefficientArrays[h[policy]/@activeLst,xVars]; (* take coefficients *)
xVals = LinearSolve[A,-b] (* get exact solution *)
], 
xVals = Chop[xVals,10^-5] (* chop the approximated solution *)
];
(* update distances with new pair values *)
updatedDist= ReplacePart[dist,
Thread[Rule[activeLst,(* associate with each active pair... *)
If[#==0,Fixed[0],#]&/@xVals] (* ...its new value, fixing those which are 0 *)
]];
(*DebugPrint["Current Policy: "MatrixForm@policy,"Current Distance: ",MatrixForm@updatedDist];*)
DebugPrint["Current over-approximation: ",# -> getDistValue[updatedDist,#]&/@activeLst];
{updatedDist,policy}
];


EmptyCoupling[mdp_]:=
Module[{sizeS,sizeA},
{sizeS,sizeA}=mdpDimensions[mdp];
SparseArray[{},{sizeS,sizeS,sizeA},0]]


getDistValue[dist_,index:{_,_}]:=
Module[{getValue},
getValue[Fixed[v_]]:=v;
getValue[v_]:=v;

getValue[Extract[dist,Sort@index]]
];


InitDistances[mdp_,estimates_]:=
Module[{sizeS=mdpDimensions[mdp][[1]],diffIndex,fixedEstim,idPairs,incPairs},
diffIndex[{s_,t_}->_]:=s!=t;
(* fixed estimates with sorted indices *)
fixedEstim=DeleteDuplicates[
Select[Thread[Rule[Sort/@estimates[[All,1]],Fixed/@estimates[[All,2]]]],diffIndex],
First@#1==First@#2&];
(* {s,s} pairs are fixed to 0 *)
idPairs=Table[{s,s}->Fixed[0],{s,sizeS}];
incPairs = (#->Fixed[1]&)/@ Select[IncompatiblePairs[mdp],#[[1]]<#[[2]]&];
(* return a matrix with the fixed estimates and set to \[Infinity] all the remaining pairs *)
SparseArray[Join[idPairs,incPairs,fixedEstim],{sizeS,sizeS},\[Infinity]]
];


RelativeCostMatrix[dist_,IndexedSchedule[mapS_,mapT_,basicCells_,V_]]:=
Module[{getCellCost,cost,max},
getCellCost[_,{i_,j_}]:=getDistValue[dist,{i/.mapS,j/.mapT}];
cost=MapIndexed[getCellCost,V,{2}];
max=Max[Select[Flatten[cost],#!= \[Infinity]&]];
cost/.\[Infinity]->max
];


(* ::Text:: *)
(*MatchingToChange[coupling,policy,dist,{s,t}] == {{i,j}, {redCost, nbcell}} if the problem (i,j) can be further minimized on its *)
(*optimal action (i.e. policy[[s,t]]), otherwise MatchingToChange[coupling,policy,dist,{s,t}] == {}*)


MatchingToChange[coupling_,policy_,dist_,queryLst_]:=
Module[{Q=queryLst,s,t,a,reducedcost,schedule,cell,siblings,toVisit,
visited=SparseArray[{},Dimensions@dist,False]},

toVisit[{u_,v_}]:=Not[visited[[u,v]]\[Or]FixedDistanceQ[dist,{u,v}]];
(* a new problem should not be visited if it has been visited already or 
its corresponding value is fixed *)
While[Length[Q] >0,
(* take a problem from the queue and set it as already visited *)
{s,t}=First@Q;visited=ReplacePart[visited,{s,t}->True];
(*DebugPrint["Visited problem:",{i,j}];*)
a = policy[[s,t]];(* action chosen from (s,t) by the current optimal policy *)
(* compute the least reduced cost for the current problem *)
schedule=coupling[[s,t,a]];
Switch[schedule,
_IndexedSchedule,
{reducedcost,cell}=AddtoBase[RelativeCostMatrix[dist,schedule],schedule];
If[reducedcost<0,
(* the first encoutered vertex to be changed *)
DebugPrint["Non optimal schedule for: ",{s,t,a},
" reduced cost: ",reducedcost, " for introducing ",cell," as basic"];
Return[{{s,t,a},{reducedcost,cell}}],
(* otherwise insert its siblings to the queue *)
siblings =Select[NextDemandedPairs[schedule],toVisit];
Q = DeleteDuplicates@Join[Rest[Q],siblings];
],
_CMatching, Return[{{s,t,a},Null}],
_, 
Print["Neither IndexedSchedule nor CMatching. ",s,"-",t,"-",a,PrPrint[schedule]];
Message[BDistMDP::Exception];Abort[]
]];
(* none of the reached problems can be further decreased *)
{}
];


Options[BDistMDP]={Verbose->False,ConsistencyCheck->True,RulesForm->True,Exact->False,Estimates-> {}};
BDistMDP::query="The following queries are ill-formed: `1`.";
BDistMDP::MDP = "The 1st argumument is not well-formed MDP.";
BDistMDP::dfInterval ="The discount factor must be in the interval (0,1)";
BDistMDP::dfFormat = "The discount factor has to be a `1` number";
BDistMDP::MultEstimates="The following estimates sets of estimates are in conflict. 
Notice that if you have both {s,t}->v1 and {t,s}->v2, v1 must be equal to v2.
`1`";
BDistMDP::WrongEstimates="Notice that each estimate should be given in the form '{s,t} \[RightArrow] v' where 1 \[LessEqual] s,t \[LessEqual] |S| and 0 \[LessEqual] v < \[Infinity].
The following estimates are ill-formed 
`1`";
BDistMDP::Exception="Something went wrong";

BDistMDP[mdp:(MDP[_,_,_]|CMDP[_,_]),\[Lambda]_, All,
options: OptionsPattern[{RulesForm->True,Verbose -> False,ConsistencyCheck->True,Estimates-> {},Exact->False}]]:=
BDistMDP[mdp,\[Lambda],AllPairs[mdp],options];
BDistMDP[mdp:(MDP[_,_,_]|CMDP[_,_]),\[Lambda]_, queryLst_, 
options:OptionsPattern[{RulesForm->True,Verbose -> False,ConsistencyCheck->True,Estimates-> {},Exact->False}]]:=
((* Set Options for ComputeDistance *)
SetOptions[BDistMDP,Exact->OptionValue[Exact]];
SetOptions[BDistMDP,RulesForm->OptionValue[RulesForm]];
SetOptions[BDistMDP,Verbose->OptionValue[Verbose]];
SetOptions[BDistMDP,ConsistencyCheck->OptionValue[ConsistencyCheck]];
SetOptions[BDistMDP,Estimates->OptionValue[Estimates]];
(* Do consistency check on input *)
If[OptionValue[ConsistencyCheck],CheckInput[mdp,\[Lambda], queryLst]];
(* Start computing the Subscript[d, \[Lambda]](s,t) *)
Switch[mdp,
_MDP, ComputeDistancesAux[mdp,\[Lambda],queryLst],
_CMDP, ComputeCompositeDistancesAux[mdp,\[Lambda], queryLst],
_,Massage[BDistMDP::Exception];Abort[]] 
);


BadQuery[MDP[_,_,_],size_][pair:{_Integer,_Integer}]:= Not[And@@(IntervalMemberQ[Interval[{1,size}],#]&/@pair)];
BadQuery[CMDP[enc_,_],size_][cpair:{_,_}]:=Not[And@@(IntervalMemberQ[Interval[{1,size}],#]&/@pairFromComponents[enc][cpair])];
BadQuery[_,_][_]:=True;


CheckInput[mdp:(MDP[_,_,_]|CMDP[_,_]),\[Lambda]_, queryLst_]:=
Module[{BadNumberFormat,BadEstimate,sizeS,
MultipleEstimates,Tagestimate,estimates,badinput,exactQ},

BadNumberFormat[x_]:=Not[MatchQ[x,_Rational|_Integer]]/;exactQ;
BadNumberFormat[x_]:=Not[MatchQ[x,_Real|_Integer]];

(*Or@@(Length[#]!=1&/@(DeleteDuplicates[#[[All,2]]]&/@GatherBy[estimates,Composition[Sort,First]]))*)
MultipleEstimates:=
Select[Reap[Sow[#,Tagestimate[mdp,#]]&/@estimates][[2]],Length[DeleteDuplicates[#[[All,2]]]]!=1&];
Tagestimate[CMDP[enc_,_],cpair_->value_]:=Sort[w@@(pairFromComponents[enc][cpair])];
Tagestimate[MDP[_,_],pair_->value_]:=Sort[w@@pair];

BadEstimate[size_][pair_->value_]:= BadQuery[mdp,size][pair] \[Or] value<0 \[Or] BadNumberFormat[value];

exactQ = OptionValue[BDistMDP,Exact];
Which[
Not[MDPQ[exactQ][mdp]], Message[BDistMDP::MDP];Abort[],
BadNumberFormat[\[Lambda]], Message[BDistMDP::dfFormat,If[exactQ,"rational","real"]];Abort[],
\[Lambda]<=0 \[Or] \[Lambda]>=1, Message[BDistMDP::dfInterval,\[Lambda]];Abort[]
];
(* MDP and discount factor are ok... now check query and estimates *)
sizeS=mdpDimensions[mdp][[1]];
estimates = OptionValue[BDistMDP,Estimates];
Which[
Length[badinput=Select[queryLst,BadQuery[mdp,sizeS]]]>0,
	Message[BDistMDP::query,badinput];Abort[],
Length[badinput=Select[estimates,BadEstimate[sizeS]]]>0,
	Message[BDistMDP::WrongEstimates,badinput];Abort[],
Length[badinput=MultipleEstimates]> 0,
	Message[BDistMDP::MultEstimates,badinput];Abort[]
];
];


ComputeDistancesAux[mdp:MDP[\[Tau]_,L_,aLbl_], \[Lambda]_, queryLst_]:=
Module[{sortedQuery,activeLst,query,FixPair,sizeA,sizeS, time,
currCoupling,currPolicy,currDist,
vtc,scheduleIndex, rcCell,oldSchedule, newSchedule,iterations=0},

FixPair[dist_,pair:{_,_}]:=ReplacePart[dist,pair->Fixed[getDistValue[dist,pair]]];

{sizeS,sizeA}=mdpDimensions[mdp];
(* Initialization *)
time =First@Timing[
(* initialize currDist with estimates *)
currDist=InitDistances[mdp,OptionValue[BDistMDP,Estimates]];
(* initialize currCoupling as empty *)
currCoupling=EmptyCoupling[mdp];
sortedQuery=DeleteDuplicates[Sort/@Select[queryLst,Not@FixedDistanceQ[currDist,#]&]];
While[sortedQuery!={},
DebugPrint["Pairs left to consider: ",sortedQuery];
(* pick at random a pair from the query *)
query=sortedQuery[[{1}]];(*sortedQuery\[LeftDoubleBracket]RandomInteger[{1,Length@sortedQuery},1]\[RightDoubleBracket];*)
DebugPrint["Picked pair: ",query];
currCoupling = AddPairs[mdp,currDist][currCoupling,query];
PrintActiveProblems[currDist,currCoupling,query];
activeLst=ActivePairs[currDist,currCoupling,query];
{currDist,currPolicy} =UpdateOptPolicyValue[\[Lambda], mdp,currDist,currCoupling,activeLst];

(* look for a non-optimal transportation schedule *)
vtc = MatchingToChange[currCoupling,currPolicy,currDist,query];
(*DebugPrint[vtc];*)
While[vtc!= {},
iterations++;
{scheduleIndex, rcCell} = vtc;(* consider the schedule to be optimized *)
(* move to a better coupling *)
oldSchedule= Extract[currCoupling,scheduleIndex];
newSchedule = BestVertex[RelativeCostMatrix[currDist,oldSchedule], rcCell, oldSchedule];
currCoupling = UpdateCoupling[mdp,currDist][currCoupling,scheduleIndex, newSchedule];

PrintActiveProblems[currDist,currCoupling,query];
(* update the current distance with the optimal value function for the current coupling *)
activeLst=ActivePairs[currDist,currCoupling,query];
{currDist,currPolicy} = UpdateOptPolicyValue[\[Lambda], mdp,currDist,currCoupling,activeLst];
(*DebugPrint[MatrixForm@currDist];*)
(* look for the next non-optimal transportation schedule *)
vtc = MatchingToChange[currCoupling,currPolicy,currDist,query];
];
(* Fix the distance for the current active pairs *)
currDist=Fold[FixPair,currDist,ActivePairs[currDist,currCoupling,query]];
(* update the query list *)
sortedQuery =Select[sortedQuery,Not@FixedDistanceQ[currDist,#]&];
]];
DebugPrint["There are no more pairs to process."];
DebugPrint["Total solved TPs: ",iterations];
DebugPrint["Execution Time: ",time, " sec"];
If[OptionValue[BDistMDP,RulesForm],
(#->getDistValue[currDist,#]&)/@queryLst,
{currCoupling,currDist}
]
];


(* ::Subsection:: *)
(*Asynchronous Parallel Composition*)


stateToComponents[{_,revmap_}][s_]:=s/.revmap;
pairToSubPairs[enc_][cpair:{_,_}]:=Thread[stateToComponents[enc]/@cpair];
stateFromSubStates[{map_,_}][substates_]:=substates/.map;
pairFromComponents[enc_][cpair:{_,_}]:=stateFromSubStates[enc]/@cpair;


ParallelMDPs[mdpLst_]:=CMDP[getEncodingMaps[mdpLst],mdpLst];


getEncodingMaps[mdpLst_]:=
Module[{counter=1,states,P,map,revmap},
states[mdp_MDP]:=Table[i,{i,mdpDimensions[mdp][[1]]}];
P[x___]:=List[x]-> counter++;
(* associate each composite state to a single state *)
map=Flatten@Outer[P,Sequence@@(states/@mdpLst)];
(* get the reverse map *)
revmap=#[[2]]->#[[1]]&/@map;
{map,revmap}
];


AddCompositePair[CMDP[enc_,mdpLst_],dist_][coupling_,pair:{_,_}]:= coupling/; VisitedPairQ[dist,coupling,pair];
AddCompositePair[mdp:CMDP[enc_,mdpLst_],dist_][coupling_,pair:{s_,t_}]:=
Module[{Marginal,TP,newtransitions},
Marginal[state_,al_]:={#[[2]],#[[1]]}&/@getDistribution[mdp,state,al];
TP[al_]:=
Module[{guess,costmatrix},
guess=GuessVertex[Marginal[s,al],Marginal[t,al]];
costmatrix=RelativeCostMatrix[dist,guess];
BestVertex[costmatrix,AddtoBase[costmatrix,guess],guess]
];
(* take the optimal TP schedule for each action *)
newtransitions= TP/@mdpActions[mdp];
(* recursively update the coupling *)
UpdateCompositeCoupling[mdp,dist][coupling,pair,newtransitions]
];


AddCompositePairs[mdp:CMDP[enc_,mdpLst_],dist_][coupling_,queryLst_]:=
Fold[AddCompositePair[mdp,dist],coupling,queryLst];


UpdateCompositeCoupling[mdp:CMDP[enc_,mdpLst_],dist_][coupling_,{s_,t_},transitions_List]:=
AddCompositePairs[mdp,dist][ReplacePart[coupling,{s,t}->transitions],NextDemandedPairs[transitions]];
UpdateCompositeCoupling[mdp:CMDP[enc_,mdpLst_],dist_][coupling_,{s_,t_,a_},schedule_IndexedSchedule]:=
AddCompositePairs[mdp,dist][ReplacePart[coupling,{s,t,a}->schedule],NextDemandedPairs[schedule]];


InitCompositeDistance[enc_][distLst_]:=
Module[{d,sizeS=Length[enc[[2]]]},
d[s_,t_]:=Module[{val},
val=Plus@@MapThread[getDistValue,{distLst,pairToSubPairs[enc][{s,t}]}];
If[val==0,Fixed[0],val]
];
SparseArray[Flatten@Table[{i,j}->d[i,j],{i,sizeS},{j,i,sizeS}],{sizeS,sizeS}]
];


(*InitCompositeDistance[mdp:CMDP[enc_,mdpLst_],\[Lambda]_,estimates_]:=
Module[{activeLst,Ccoupling,sizeS,dist,couplingLst},
sizeS=First@mdpDimensions[mdp];
(* compute distance for each subcomponent in parallel *)
couplingLst=ParallelMap[BDistMDP[#,\[Lambda], All,RulesForm->False,ConsistencyCheck->False]&,mdpLst];
Print[couplingLst];
Ccoupling=ComposeCouplings[mdp][couplingLst];
Print[Ccoupling];
activeLst=Join@@Table[{i,j},{i,sizeS},{j,i+1,sizeS}];
dist=First@UpdateOptPolicyValue[\[Lambda],mdp,InitDistances[mdp,estimates],Ccoupling,activeLst];
Print[dist];
Abort[];
];*)


ComposeCouplings[mdp:CMDP[enc_,mdpLst_]][couplingLst_]:=
Module[{bCell,matchingTransitions,SelectAction,composeCellsRules,ComposeSchedule,cmdpActions,sizeS,sizeA},

bCell[IndexedSchedule[mapS_,mapT_,basicCells_,V_]][{i_,j_}]:=
{i/.mapS,j/.mapT}->EpsilonRemoval[V[[i,j]]];
matchingTransitions[schedule:IndexedSchedule[mapS_,mapT_,basicCells_,V_]]:=
Select[bCell[schedule]/@basicCells,#[[2]]>0&];

SelectAction[al_][{u_,u_},{mdpId_}]:=
({#[[1]],#[[1]]}->#[[2]]&)/@getDistribution[mdpLst[[mdpId]],u,al];
SelectAction[al_][{u_,v_},{mdpId_}]:=SelectAction[al][{v,u},{mdpId}]/;u>v;
SelectAction[al_][{u_,v_},{mdpId_}]:=
Module[{act=Position[mdpActions[mdpLst[[mdpId]]],al]},
If[act=={},{{u,v}->1},
matchingTransitions[couplingLst[[mdpId,u,v,act[[1,1]]]]]
]];

composeCellsRules[cellsRules_]:=
pairFromComponents[enc][{cellsRules[[All,1,1]], cellsRules[[All,1,2]]}]->Times@@cellsRules[[All,2]];

ComposeSchedule[s_,t_,al_]:=
Module[{subpairs,subMatchings},
subpairs=pairToSubPairs[enc][{s,t}];
(* take the list of subMathings related to the 
composite pair {s,t} and action label al*)
subMatchings=MapIndexed[SelectAction[al],subpairs];
CMatching[composeCellsRules/@Distribute[subMatchings,List]]
];

cmdpActions=mdpActions[mdp];
{sizeS,sizeA}=mdpDimensions[mdp];

SparseArray[
Flatten@Table[{s,t,a}->ComposeSchedule[s,t,cmdpActions[[a]]],
{s,sizeS},{t,s+1,sizeS},{a,sizeA}],{sizeS,sizeS,sizeA}]

];


(* enc=getEncodingMaps@mdpLst;  composite states encoding *)
ComputeCompositeDistancesAux[mdp:CMDP[enc_,mdpLst_],\[Lambda]_, queryLst_]:=
Module[{sortedQuery,activeLst,query,FixPair,Marginal,time,
couplingLst,distLst, actions=mdpActions[mdp],
currCoupling,currPolicy,currDist,currData,exactQ,
vtc,scheduleIndex, rcCell,oldSchedule, newSchedule,iterations=0},

FixPair[dist_,pair:{_,_}]:=ReplacePart[dist,pair->Fixed[getDistValue[dist,pair]]];
Marginal[state_,a_]:={#[[2]],#[[1]]}&/@getDistribution[mdp,state,actions[[a]]];

time =First@Timing[
(* initialize currCoupling as the composition of the optimal couplings of the components *)
exactQ=OptionValue[BDistMDP,Exact]; 
{couplingLst,distLst}=Thread[ParallelMap[BDistMDP[#,\[Lambda], All,Exact->exactQ,RulesForm->False,ConsistencyCheck->False]&,mdpLst]];
currCoupling=ComposeCouplings[mdp][couplingLst];
(* initialize currDist with estimates *)
currDist=InitCompositeDistance[enc][distLst];(*InitDistances[mdp,OptionValue[BDistMDP,Estimates]];*)
(* Initialization *)
sortedQuery=DeleteDuplicates[Sort/@Select[pairFromComponents[enc]/@queryLst,Not@FixedDistanceQ[currDist,#]&]];

While[sortedQuery!={},
DebugPrint["Pairs left to consider: ",sortedQuery];
(* pick at random a pair from the query *)
query=sortedQuery[[{1}]];(*sortedQuery\[LeftDoubleBracket]RandomInteger[{1,Length@sortedQuery},1]\[RightDoubleBracket];*)
DebugPrint["Picked pair: ",query];

currCoupling = AddCompositePairs[mdp,currDist][currCoupling,query];
PrintActiveProblems[currDist,currCoupling,query];
activeLst=ActivePairs[currDist,currCoupling,query];
{currDist,currPolicy}=UpdateOptPolicyValue[\[Lambda], mdp,currDist,currCoupling,activeLst];


(*DebugPrint[MatrixForm@currDist];*)
(* look for a non-optimal transportation schedule *)
vtc = MatchingToChange[currCoupling,currPolicy,currDist,query];
While[vtc!= {},
iterations++;
{scheduleIndex, rcCell} = vtc;(* consider the schedule to be optimized *)
Switch[rcCell,
Null,(* the schedule is not an IndexedSchedule *)
newSchedule=Module[{s,t,a,guess,costmatrix},
{s,t,a}=scheduleIndex;
guess=GuessVertex[Marginal[s,a],Marginal[t,a]];
costmatrix=RelativeCostMatrix[currDist,guess];
BestVertex[costmatrix,AddtoBase[costmatrix,guess],guess]
]; (* contruct an indexed *)
currCoupling = UpdateCompositeCoupling[mdp,currDist][currCoupling,scheduleIndex, newSchedule],
_, 
oldSchedule=Extract[currCoupling,scheduleIndex];
(* move to a better coupling *)
newSchedule = BestVertex[RelativeCostMatrix[currDist,oldSchedule], rcCell, oldSchedule];
currCoupling = UpdateCompositeCoupling[mdp,currDist][currCoupling,scheduleIndex, newSchedule];
PrintActiveProblems[currDist,currCoupling,query];
];
(* update the current distance with the optimal value function for the current coupling *)
activeLst=ActivePairs[currDist,currCoupling,query];
(*Print[activeLst];*)
{currDist,currPolicy} = UpdateOptPolicyValue[\[Lambda], mdp,currDist,currCoupling,activeLst];
(*DebugPrint[MatrixForm@currDist];*)
(* look for the next non-optimal transportation schedule *)
vtc = MatchingToChange[currCoupling,currPolicy,currDist,query];
];
(* Fix the distance for the current active pairs *)

currDist=Fold[FixPair,currDist,ActivePairs[currDist,currCoupling,query[[{1}]]]];
(*DebugPrint[MatrixForm@currDist];*)
(* update the query list *)
sortedQuery =Select[sortedQuery,Not@FixedDistanceQ[currDist,#]&];
]];
DebugPrint["There are no more pairs to process."];
DebugPrint["Total solved TPs: ",iterations];
DebugPrint["Execution Time: ",time, " sec"];
(#->getDistValue[currDist,pairFromComponents[enc][#]]&)/@queryLst
];


(* ::Subsection:: *)
(*Bisimilarity Quotient*)


BisimQuotientMDP[mdp:MDP[\[Tau]_,\[Rho]_,aLbl_]]:=
Module[{Rate,Quo,A,b\[Tau],b\[Rho]},
A=mdpDimensions[mdp][[2]];
Quo=BisimClassesMDP[mdp];
Rate[repclass1_,a_,class2_]:=Plus@@(\[Tau][[repclass1,a,#]]&/@class2);
b\[Tau]=SparseArray@Table[Rate[First@class1,a,class2],{class1,\!\(TraditionalForm\`Quo\)},{a,A},{class2,Quo}];
b\[Rho]=SparseArray@Table[\[Rho][[First@class,a]],{class,Quo},{a,A}];

MDP[b\[Tau],b\[Rho],aLbl]
];


BisimClassesMDP[mdp:MDP[\[Tau]_,\[Rho]_,aLbl_]]:=
\!\(TraditionalForm\`FixedPoint\)[(Union@@#&)/@Gather[#,Intersection[#1,#2]!= {}&]&,
Select[BDistMDP[mdp,0.5,All,Verbose->False],#[[2]]==0&][[All,1]]];


End[]


EndPackage[]
