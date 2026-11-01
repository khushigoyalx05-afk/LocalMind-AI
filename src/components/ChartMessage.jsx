
import React, { useMemo } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import ChartRenderer from "./ChartRenderer.jsx";

const SUPPORTED_CHARTS = new Set([
  "line",
  "bar",
  "pie",
  "scatter",
]);

function validateChart(value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return null;
  }

  if (value.type && value.type !== "chart") {
    return null;
  }

  const chartType = String(value.chartType || "").toLowerCase();

  if (!SUPPORTED_CHARTS.has(chartType)) {
    return null;
  }

  const xKey = value.xKey;
  const yKey = value.yKey;

  if (
    typeof xKey !== "string" ||
    typeof yKey !== "string" ||
    !xKey.trim() ||
    !yKey.trim()
  ) {
    return null;
  }

  if (
    !Array.isArray(value.data) ||
    value.data.length === 0 ||
    value.data.length > 1000
  ) {
    return null;
  }

  const data = value.data
    .filter(
      (row) =>
        row &&
        typeof row === "object" &&
        !Array.isArray(row)
    )
    .map((row) => {
      const rawY = row[yKey];
      const rawX = row[xKey];

      if (
        rawY == null ||
        rawY === "" ||
        rawX == null ||
        rawX === ""
      ) {
        return null;
      }

      const numericY = Number(rawY);

      if (!Number.isFinite(numericY)) {
        return null;
      }

      if (chartType === "pie" && numericY < 0) {
        return null;
      }

      if (chartType === "scatter") {
        const numericX = Number(rawX);

        if (!Number.isFinite(numericX)) {
          return null;
        }

        return {
          ...row,
          [xKey]: numericX,
          [yKey]: numericY,
        };
      }

      return {
        ...row,
        [yKey]: numericY,
      };
    })
    .filter(Boolean);

  if (data.length === 0) {
    return null;
  }

  return {
    ...value,
    type: "chart",
    chartType,
    xKey,
    yKey,
    data,
  };
}

function parseChart(raw) {
  try {
    return validateChart(JSON.parse(raw.trim()));
  } catch {
    return null;
  }
}

function splitContent(content) {
  const text = String(content ?? "");
  const segments = [];

  const chartRegex = /```chart-data\s*([\s\S]*?)```/gi;

  let lastIndex = 0;
  let match;

  while ((match = chartRegex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({
        type: "markdown",
        content: text.slice(lastIndex, match.index),
      });
    }

    const chart = parseChart(match[1]);

    if (chart) {
      segments.push({
        type: "chart",
        chart,
      });
    } else {
      segments.push({
        type: "markdown",
        content: match[0],
      });
    }

    lastIndex = chartRegex.lastIndex;
  }

  if (lastIndex < text.length) {
    segments.push({
      type: "markdown",
      content: text.slice(lastIndex),
    });
  }

  // Also support an entire response containing only chart JSON.
  if (
    segments.length === 1 &&
    segments[0].type === "markdown"
  ) {
    const chart = parseChart(text);

    if (chart) {
      return [{ type: "chart", chart }];
    }
  }

  return segments;
}

export default function ChartMessage({ content = "" }) {
  const segments = useMemo(
    () => splitContent(content),
    [content]
  );

  return (
    <div className="chart-message">
      {segments.map((segment, index) => {
        if (segment.type === "chart") {
          return (
            <ChartRenderer
              key={`chart-${index}`}
              chart={segment.chart}
            />
          );
        }

        return (
          <ReactMarkdown
            key={`markdown-${index}`}
            remarkPlugins={[remarkGfm]}
          >
            {segment.content}
          </ReactMarkdown>
        );
      })}
    </div>
  );
}
