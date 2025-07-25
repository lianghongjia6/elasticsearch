/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.expression.function.aggregate;

import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.xpack.esql.core.expression.Expression;
import org.elasticsearch.xpack.esql.core.expression.Literal;
import org.elasticsearch.xpack.esql.core.tree.NodeInfo;
import org.elasticsearch.xpack.esql.core.tree.Source;
import org.elasticsearch.xpack.esql.core.type.DataType;
import org.elasticsearch.xpack.esql.expression.SurrogateExpression;
import org.elasticsearch.xpack.esql.expression.function.Example;
import org.elasticsearch.xpack.esql.expression.function.FunctionAppliesTo;
import org.elasticsearch.xpack.esql.expression.function.FunctionAppliesToLifecycle;
import org.elasticsearch.xpack.esql.expression.function.FunctionInfo;
import org.elasticsearch.xpack.esql.expression.function.FunctionType;
import org.elasticsearch.xpack.esql.expression.function.Param;
import org.elasticsearch.xpack.esql.expression.predicate.operator.arithmetic.Div;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Similar to {@link Avg}, but it is used to calculate the average value over a time series of values from the given field.
 */
public class AvgOverTime extends TimeSeriesAggregateFunction implements SurrogateExpression {
    public static final NamedWriteableRegistry.Entry ENTRY = new NamedWriteableRegistry.Entry(
        Expression.class,
        "AvgOverTime",
        AvgOverTime::new
    );

    @FunctionInfo(
        returnType = "double",
        description = "The average over time of a numeric field.",
        type = FunctionType.TIME_SERIES_AGGREGATE,
        appliesTo = { @FunctionAppliesTo(lifeCycle = FunctionAppliesToLifecycle.UNAVAILABLE) },
        note = "Available with the [TS](/reference/query-languages/esql/commands/source-commands.md#esql-ts) command in snapshot builds",
        examples = { @Example(file = "k8s-timeseries", tag = "avg_over_time") }
    )
    public AvgOverTime(
        Source source,
        @Param(
            name = "number",
            type = { "double", "integer", "long" },
            description = "Expression that outputs values to average."
        ) Expression field
    ) {
        this(source, field, Literal.TRUE);
    }

    public AvgOverTime(Source source, Expression field, Expression filter) {
        super(source, field, filter, emptyList());
    }

    private AvgOverTime(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    protected TypeResolution resolveType() {
        return perTimeSeriesAggregation().resolveType();
    }

    @Override
    public String getWriteableName() {
        return ENTRY.name;
    }

    @Override
    public DataType dataType() {
        return perTimeSeriesAggregation().dataType();
    }

    @Override
    protected NodeInfo<AvgOverTime> info() {
        return NodeInfo.create(this, AvgOverTime::new, field(), filter());
    }

    @Override
    public AvgOverTime replaceChildren(List<Expression> newChildren) {
        return new AvgOverTime(source(), newChildren.get(0), newChildren.get(1));
    }

    @Override
    public AvgOverTime withFilter(Expression filter) {
        return new AvgOverTime(source(), field(), filter);
    }

    @Override
    public Expression surrogate() {
        Source s = source();
        Expression f = field();
        return new Div(s, new SumOverTime(s, f, filter()), new CountOverTime(s, f, filter()), dataType());
    }

    @Override
    public AggregateFunction perTimeSeriesAggregation() {
        return new Avg(source(), field(), filter());
    }
}
