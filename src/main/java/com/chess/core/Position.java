package com.chess.core;

import java.util.Objects;

public class Position {
    private final int row;
    private final int column;

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getRow() { return row; }
    public int getColumn() { return column; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Position)) return false;
        Position that = (Position) other;
        return this.row == that.row && this.column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "Position(" + row + "," + column + ")";
    }
}