
import React from "react";

import {
  ResponsiveContainer,
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  ScatterChart,
  Scatter,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";

const COLORS = [
  "#a78bfa",
  "#22d3ee",
  "#60a5fa",
  "#f472b6",
  "#34d399",
  "#fbbf24",
  "#fb7185",
  "#818cf8",
];

const AXIS_COLOR = "#94a3b8";
const GRID_COLOR = "rgba(148,163,184,0.14)";

const tooltipStyle = {
  backgroundColor: "#111827",
  border: "1px solid rgba(167,139,250,0.35)",
  borderRadius: "12px",
  color: "#f8fafc",
};

function formatValue(value) {
  if (typeof value !== "number") return String(value ?? "");

  return new Intl.NumberFormat("en-IN", {
    maximumFractionDigits: 2,
  }).format(value);
}

function ChartTooltip() {
  return (
    <Tooltip
      contentStyle={tooltipStyle}
      labelStyle={{ color: "#e2e8f0" }}
      formatter={(value) => formatValue(value)}
    />
  );
}

export default function ChartRenderer({ chart }) {
  if (!chart || !Array.isArray(chart.data) || !chart.data.length) {
    return (
      <div className="chart-error">
        No valid chart data available.
      </div>
    );
  }

  const {
    chartType,
    xKey,
    yKey,
    data,
    title,
    aggregation,
  } = chart;

  const commonAxis = {
    tick: { fill: AXIS_COLOR, fontSize: 11 },
    tickLine: false,
    axisLine: false,
  };

  const chartTitle =
    title ||
    `${yKey} by ${xKey}`;

  function renderChart() {
    switch (chartType) {
      case "bar":
        return (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={data}
              margin={{
                top: 12,
                right: 16,
                left: 0,
                bottom: 12,
              }}
            >
              <CartesianGrid
                stroke={GRID_COLOR}
                strokeDasharray="4 4"
                vertical={false}
              />

              <XAxis
                dataKey={xKey}
                {...commonAxis}
                interval={0}
                minTickGap={12}
              />

              <YAxis
                {...commonAxis}
                tickFormatter={formatValue}
                width={65}
              />

              <ChartTooltip />

              <Legend />

              <Bar
                dataKey={yKey}
                fill={COLORS[0]}
                radius={[6, 6, 0, 0]}
                maxBarSize={65}
                animationDuration={700}
              />
            </BarChart>
          </ResponsiveContainer>
        );

      case "line":
        return (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart
              data={data}
              margin={{
                top: 12,
                right: 16,
                left: 0,
                bottom: 12,
              }}
            >
              <CartesianGrid
                stroke={GRID_COLOR}
                strokeDasharray="4 4"
                vertical={false}
              />

              <XAxis
                dataKey={xKey}
                {...commonAxis}
              />

              <YAxis
                {...commonAxis}
                tickFormatter={formatValue}
                width={65}
              />

              <ChartTooltip />

              <Legend />

              <Line
                type="monotone"
                dataKey={yKey}
                stroke={COLORS[0]}
                strokeWidth={3}
                dot={{
                  fill: COLORS[0],
                  stroke: "#111827",
                  strokeWidth: 2,
                  r: 4,
                }}
                activeDot={{ r: 7 }}
                animationDuration={700}
              />
            </LineChart>
          </ResponsiveContainer>
        );

      case "pie":
        return (
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                dataKey={yKey}
                nameKey={xKey}
                cx="50%"
                cy="46%"
                outerRadius="75%"
                innerRadius="38%"
                paddingAngle={2}
                animationDuration={700}
              >
                {data.map((_, index) => (
                  <Cell
                    key={`slice-${index}`}
                    fill={COLORS[index % COLORS.length]}
                  />
                ))}
              </Pie>

              <ChartTooltip />

              <Legend
                verticalAlign="bottom"
                height={36}
              />
            </PieChart>
          </ResponsiveContainer>
        );

      case "scatter":
        return (
          <ResponsiveContainer width="100%" height="100%">
            <ScatterChart
              margin={{
                top: 12,
                right: 16,
                left: 0,
                bottom: 12,
              }}
            >
              <CartesianGrid
                stroke={GRID_COLOR}
                strokeDasharray="4 4"
              />

              <XAxis
                type="number"
                dataKey={xKey}
                name={xKey}
                {...commonAxis}
                tickFormatter={formatValue}
              />

              <YAxis
                type="number"
                dataKey={yKey}
                name={yKey}
                {...commonAxis}
                tickFormatter={formatValue}
                width={65}
              />

              <ChartTooltip />

              <Legend />

              <Scatter
                name={chartTitle}
                data={data}
                fill={COLORS[1]}
                animationDuration={700}
              />
            </ScatterChart>
          </ResponsiveContainer>
        );

      default:
        return (
          <div className="chart-error">
            Unsupported chart type: {chartType}
          </div>
        );
    }
  }

  return (
    <div className="chart-container">
      <div className="chart-header">
        <div>
          <h3>{chartTitle}</h3>

          <p className="chart-description">
            {xKey} vs {yKey}
          </p>
        </div>

        <span className="chart-type-badge">
          {chartType}
        </span>
      </div>

      <div className="chart-wrapper">
        {renderChart()}
      </div>

      <div className="chart-footer">
        <span className="chart-data-count">
          {data.length} data points
        </span>

        {aggregation && (
          <span>
            Aggregation: {aggregation}
          </span>
        )}
      </div>
    </div>
  );
}
