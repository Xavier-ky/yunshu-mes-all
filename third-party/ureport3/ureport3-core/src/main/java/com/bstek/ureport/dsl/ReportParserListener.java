// Generated from ReportParser.g4 by ANTLR 4.5.3
package com.bstek.ureport.dsl;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ReportParserParser}.
 */
public interface ReportParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#entry}.
	 * @param ctx the parse tree
	 */
	void enterEntry(ReportParserParser.EntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#entry}.
	 * @param ctx the parse tree
	 */
	void exitEntry(ReportParserParser.EntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ReportParserParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ReportParserParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code complexExprComposite}
	 * labeled alternative in {@link ReportParserParser#exprComposite}.
	 * @param ctx the parse tree
	 */
	void enterComplexExprComposite(ReportParserParser.ComplexExprCompositeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code complexExprComposite}
	 * labeled alternative in {@link ReportParserParser#exprComposite}.
	 * @param ctx the parse tree
	 */
	void exitComplexExprComposite(ReportParserParser.ComplexExprCompositeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleExprComposite}
	 * labeled alternative in {@link ReportParserParser#exprComposite}.
	 * @param ctx the parse tree
	 */
	void enterSingleExprComposite(ReportParserParser.SingleExprCompositeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleExprComposite}
	 * labeled alternative in {@link ReportParserParser#exprComposite}.
	 * @param ctx the parse tree
	 */
	void exitSingleExprComposite(ReportParserParser.SingleExprCompositeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenExprComposite}
	 * labeled alternative in {@link ReportParserParser#exprComposite}.
	 * @param ctx the parse tree
	 */
	void enterParenExprComposite(ReportParserParser.ParenExprCompositeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenExprComposite}
	 * labeled alternative in {@link ReportParserParser#exprComposite}.
	 * @param ctx the parse tree
	 */
	void exitParenExprComposite(ReportParserParser.ParenExprCompositeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ternaryExprComposite}
	 * labeled alternative in {@link ReportParserParser#exprComposite}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExprComposite(ReportParserParser.TernaryExprCompositeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ternaryExprComposite}
	 * labeled alternative in {@link ReportParserParser#exprComposite}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExprComposite(ReportParserParser.TernaryExprCompositeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#ternaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExpr(ReportParserParser.TernaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#ternaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExpr(ReportParserParser.TernaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#caseExpr}.
	 * @param ctx the parse tree
	 */
	void enterCaseExpr(ReportParserParser.CaseExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#caseExpr}.
	 * @param ctx the parse tree
	 */
	void exitCaseExpr(ReportParserParser.CaseExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#casePart}.
	 * @param ctx the parse tree
	 */
	void enterCasePart(ReportParserParser.CasePartContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#casePart}.
	 * @param ctx the parse tree
	 */
	void exitCasePart(ReportParserParser.CasePartContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#ifExpr}.
	 * @param ctx the parse tree
	 */
	void enterIfExpr(ReportParserParser.IfExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#ifExpr}.
	 * @param ctx the parse tree
	 */
	void exitIfExpr(ReportParserParser.IfExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#ifPart}.
	 * @param ctx the parse tree
	 */
	void enterIfPart(ReportParserParser.IfPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#ifPart}.
	 * @param ctx the parse tree
	 */
	void exitIfPart(ReportParserParser.IfPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#elseIfPart}.
	 * @param ctx the parse tree
	 */
	void enterElseIfPart(ReportParserParser.ElseIfPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#elseIfPart}.
	 * @param ctx the parse tree
	 */
	void exitElseIfPart(ReportParserParser.ElseIfPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#elsePart}.
	 * @param ctx the parse tree
	 */
	void enterElsePart(ReportParserParser.ElsePartContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#elsePart}.
	 * @param ctx the parse tree
	 */
	void exitElsePart(ReportParserParser.ElsePartContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(ReportParserParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(ReportParserParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#exprBlock}.
	 * @param ctx the parse tree
	 */
	void enterExprBlock(ReportParserParser.ExprBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#exprBlock}.
	 * @param ctx the parse tree
	 */
	void exitExprBlock(ReportParserParser.ExprBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#returnExpr}.
	 * @param ctx the parse tree
	 */
	void enterReturnExpr(ReportParserParser.ReturnExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#returnExpr}.
	 * @param ctx the parse tree
	 */
	void exitReturnExpr(ReportParserParser.ReturnExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(ReportParserParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(ReportParserParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#ifCondition}.
	 * @param ctx the parse tree
	 */
	void enterIfCondition(ReportParserParser.IfConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#ifCondition}.
	 * @param ctx the parse tree
	 */
	void exitIfCondition(ReportParserParser.IfConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#variableAssign}.
	 * @param ctx the parse tree
	 */
	void enterVariableAssign(ReportParserParser.VariableAssignContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#variableAssign}.
	 * @param ctx the parse tree
	 */
	void exitVariableAssign(ReportParserParser.VariableAssignContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleJoin}
	 * labeled alternative in {@link ReportParserParser#item}.
	 * @param ctx the parse tree
	 */
	void enterSimpleJoin(ReportParserParser.SimpleJoinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleJoin}
	 * labeled alternative in {@link ReportParserParser#item}.
	 * @param ctx the parse tree
	 */
	void exitSimpleJoin(ReportParserParser.SimpleJoinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleParenJoin}
	 * labeled alternative in {@link ReportParserParser#item}.
	 * @param ctx the parse tree
	 */
	void enterSingleParenJoin(ReportParserParser.SingleParenJoinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleParenJoin}
	 * labeled alternative in {@link ReportParserParser#item}.
	 * @param ctx the parse tree
	 */
	void exitSingleParenJoin(ReportParserParser.SingleParenJoinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenJoin}
	 * labeled alternative in {@link ReportParserParser#item}.
	 * @param ctx the parse tree
	 */
	void enterParenJoin(ReportParserParser.ParenJoinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenJoin}
	 * labeled alternative in {@link ReportParserParser#item}.
	 * @param ctx the parse tree
	 */
	void exitParenJoin(ReportParserParser.ParenJoinContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#unit}.
	 * @param ctx the parse tree
	 */
	void enterUnit(ReportParserParser.UnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#unit}.
	 * @param ctx the parse tree
	 */
	void exitUnit(ReportParserParser.UnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(ReportParserParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(ReportParserParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#cellPosition}.
	 * @param ctx the parse tree
	 */
	void enterCellPosition(ReportParserParser.CellPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#cellPosition}.
	 * @param ctx the parse tree
	 */
	void exitCellPosition(ReportParserParser.CellPositionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#relativeCell}.
	 * @param ctx the parse tree
	 */
	void enterRelativeCell(ReportParserParser.RelativeCellContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#relativeCell}.
	 * @param ctx the parse tree
	 */
	void exitRelativeCell(ReportParserParser.RelativeCellContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#currentCellValue}.
	 * @param ctx the parse tree
	 */
	void enterCurrentCellValue(ReportParserParser.CurrentCellValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#currentCellValue}.
	 * @param ctx the parse tree
	 */
	void exitCurrentCellValue(ReportParserParser.CurrentCellValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#currentCellData}.
	 * @param ctx the parse tree
	 */
	void enterCurrentCellData(ReportParserParser.CurrentCellDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#currentCellData}.
	 * @param ctx the parse tree
	 */
	void exitCurrentCellData(ReportParserParser.CurrentCellDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#cell}.
	 * @param ctx the parse tree
	 */
	void enterCell(ReportParserParser.CellContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#cell}.
	 * @param ctx the parse tree
	 */
	void exitCell(ReportParserParser.CellContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#dataset}.
	 * @param ctx the parse tree
	 */
	void enterDataset(ReportParserParser.DatasetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#dataset}.
	 * @param ctx the parse tree
	 */
	void exitDataset(ReportParserParser.DatasetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#function}.
	 * @param ctx the parse tree
	 */
	void enterFunction(ReportParserParser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#function}.
	 * @param ctx the parse tree
	 */
	void exitFunction(ReportParserParser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#functionParameter}.
	 * @param ctx the parse tree
	 */
	void enterFunctionParameter(ReportParserParser.FunctionParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#functionParameter}.
	 * @param ctx the parse tree
	 */
	void exitFunctionParameter(ReportParserParser.FunctionParameterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cellPair}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterCellPair(ReportParserParser.CellPairContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cellPair}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitCellPair(ReportParserParser.CellPairContext ctx);
	/**
	 * Enter a parse tree produced by the {@code wholeCell}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterWholeCell(ReportParserParser.WholeCellContext ctx);
	/**
	 * Exit a parse tree produced by the {@code wholeCell}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitWholeCell(ReportParserParser.WholeCellContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cellCoordinateCondition}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterCellCoordinateCondition(ReportParserParser.CellCoordinateConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cellCoordinateCondition}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitCellCoordinateCondition(ReportParserParser.CellCoordinateConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleCellCondition}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterSingleCellCondition(ReportParserParser.SingleCellConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleCellCondition}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitSingleCellCondition(ReportParserParser.SingleCellConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleCell}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterSingleCell(ReportParserParser.SingleCellContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleCell}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitSingleCell(ReportParserParser.SingleCellContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleData}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterSimpleData(ReportParserParser.SimpleDataContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleData}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitSimpleData(ReportParserParser.SimpleDataContext ctx);
	/**
	 * Enter a parse tree produced by the {@code range}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterRange(ReportParserParser.RangeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code range}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitRange(ReportParserParser.RangeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleCellCoordinate}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void enterSingleCellCoordinate(ReportParserParser.SingleCellCoordinateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleCellCoordinate}
	 * labeled alternative in {@link ReportParserParser#set}.
	 * @param ctx the parse tree
	 */
	void exitSingleCellCoordinate(ReportParserParser.SingleCellCoordinateContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#cellCoordinate}.
	 * @param ctx the parse tree
	 */
	void enterCellCoordinate(ReportParserParser.CellCoordinateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#cellCoordinate}.
	 * @param ctx the parse tree
	 */
	void exitCellCoordinate(ReportParserParser.CellCoordinateContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#coordinate}.
	 * @param ctx the parse tree
	 */
	void enterCoordinate(ReportParserParser.CoordinateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#coordinate}.
	 * @param ctx the parse tree
	 */
	void exitCoordinate(ReportParserParser.CoordinateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code relative}
	 * labeled alternative in {@link ReportParserParser#cellIndicator}.
	 * @param ctx the parse tree
	 */
	void enterRelative(ReportParserParser.RelativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code relative}
	 * labeled alternative in {@link ReportParserParser#cellIndicator}.
	 * @param ctx the parse tree
	 */
	void exitRelative(ReportParserParser.RelativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code absolute}
	 * labeled alternative in {@link ReportParserParser#cellIndicator}.
	 * @param ctx the parse tree
	 */
	void enterAbsolute(ReportParserParser.AbsoluteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code absolute}
	 * labeled alternative in {@link ReportParserParser#cellIndicator}.
	 * @param ctx the parse tree
	 */
	void exitAbsolute(ReportParserParser.AbsoluteContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#conditions}.
	 * @param ctx the parse tree
	 */
	void enterConditions(ReportParserParser.ConditionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#conditions}.
	 * @param ctx the parse tree
	 */
	void exitConditions(ReportParserParser.ConditionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cellNameExprCondition}
	 * labeled alternative in {@link ReportParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCellNameExprCondition(ReportParserParser.CellNameExprConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cellNameExprCondition}
	 * labeled alternative in {@link ReportParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCellNameExprCondition(ReportParserParser.CellNameExprConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code propertyCondition}
	 * labeled alternative in {@link ReportParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterPropertyCondition(ReportParserParser.PropertyConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code propertyCondition}
	 * labeled alternative in {@link ReportParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitPropertyCondition(ReportParserParser.PropertyConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentValueCondition}
	 * labeled alternative in {@link ReportParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCurrentValueCondition(ReportParserParser.CurrentValueConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentValueCondition}
	 * labeled alternative in {@link ReportParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCurrentValueCondition(ReportParserParser.CurrentValueConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprCondition}
	 * labeled alternative in {@link ReportParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterExprCondition(ReportParserParser.ExprConditionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprCondition}
	 * labeled alternative in {@link ReportParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitExprCondition(ReportParserParser.ExprConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#property}.
	 * @param ctx the parse tree
	 */
	void enterProperty(ReportParserParser.PropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#property}.
	 * @param ctx the parse tree
	 */
	void exitProperty(ReportParserParser.PropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#currentValue}.
	 * @param ctx the parse tree
	 */
	void enterCurrentValue(ReportParserParser.CurrentValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#currentValue}.
	 * @param ctx the parse tree
	 */
	void exitCurrentValue(ReportParserParser.CurrentValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#simpleValue}.
	 * @param ctx the parse tree
	 */
	void enterSimpleValue(ReportParserParser.SimpleValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#simpleValue}.
	 * @param ctx the parse tree
	 */
	void exitSimpleValue(ReportParserParser.SimpleValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#join}.
	 * @param ctx the parse tree
	 */
	void enterJoin(ReportParserParser.JoinContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#join}.
	 * @param ctx the parse tree
	 */
	void exitJoin(ReportParserParser.JoinContext ctx);
	/**
	 * Enter a parse tree produced by {@link ReportParserParser#aggregate}.
	 * @param ctx the parse tree
	 */
	void enterAggregate(ReportParserParser.AggregateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ReportParserParser#aggregate}.
	 * @param ctx the parse tree
	 */
	void exitAggregate(ReportParserParser.AggregateContext ctx);
}