/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.mpp.aggregation;

import org.apache.iotdb.db.mpp.execution.operator.window.IWindow;
import org.apache.iotdb.tsfile.exception.write.UnSupportedDataTypeException;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.statistics.Statistics;
import org.apache.iotdb.tsfile.read.common.block.column.Column;
import org.apache.iotdb.tsfile.read.common.block.column.ColumnBuilder;
import org.apache.iotdb.tsfile.utils.TsPrimitiveType;

import static com.google.common.base.Preconditions.checkArgument;

public class MaxValueAccumulator implements Accumulator {

  private TSDataType seriesDataType;
  private TsPrimitiveType maxResult;
  private boolean initResult;

  public MaxValueAccumulator(TSDataType seriesDataType) {
    this.seriesDataType = seriesDataType;
    this.maxResult = TsPrimitiveType.getByType(seriesDataType);
  }

  // Column should be like: | ControlColumn | Time | Value |
  @Override
  public int addInput(Column[] column, IWindow curWindow, boolean ignoringNull) {
    switch (seriesDataType) {
      case INT32:
        return addIntInput(column, curWindow, ignoringNull);
      case INT64:
        return addLongInput(column, curWindow, ignoringNull);
      case FLOAT:
        return addFloatInput(column, curWindow, ignoringNull);
      case DOUBLE:
        return addDoubleInput(column, curWindow, ignoringNull);
      case TEXT:
      case BOOLEAN:
      default:
        throw new UnSupportedDataTypeException(
            String.format("Unsupported data type in MaxValue: %s", seriesDataType));
    }
  }

  // partialResult should be like: | partialMaxValue1 |
  @Override
  public void addIntermediate(Column[] partialResult) {
    checkArgument(partialResult.length == 1, "partialResult of MaxValue should be 1");
    if (partialResult[0].isNull(0)) {
      return;
    }
    switch (seriesDataType) {
      case INT32:
        updateIntResult(partialResult[0].getInt(0));
        break;
      case INT64:
        updateLongResult(partialResult[0].getLong(0));
        break;
      case FLOAT:
        updateFloatResult(partialResult[0].getFloat(0));
        break;
      case DOUBLE:
        updateDoubleResult(partialResult[0].getDouble(0));
        break;
      case TEXT:
      case BOOLEAN:
      default:
        throw new UnSupportedDataTypeException(
            String.format("Unsupported data type in MaxValue: %s", seriesDataType));
    }
  }

  @Override
  public void addStatistics(Statistics statistics) {
    if (statistics == null) {
      return;
    }
    switch (seriesDataType) {
      case INT32:
        updateIntResult((int) statistics.getMaxValue());
        break;
      case INT64:
        updateLongResult((long) statistics.getMaxValue());
        break;
      case FLOAT:
        updateFloatResult((float) statistics.getMaxValue());
        break;
      case DOUBLE:
        updateDoubleResult((double) statistics.getMaxValue());
        break;
      case TEXT:
      case BOOLEAN:
      default:
        throw new UnSupportedDataTypeException(
            String.format("Unsupported data type in MaxValue: %s", seriesDataType));
    }
  }

  // finalResult should be single column, like: | finalCountValue |
  @Override
  public void setFinal(Column finalResult) {
    if (finalResult.isNull(0)) {
      return;
    }
    initResult = true;
    switch (seriesDataType) {
      case INT32:
        maxResult.setInt(finalResult.getInt(0));
        break;
      case INT64:
        maxResult.setLong(finalResult.getLong(0));
        break;
      case FLOAT:
        maxResult.setFloat(finalResult.getFloat(0));
        break;
      case DOUBLE:
        maxResult.setDouble(finalResult.getDouble(0));
        break;
      case TEXT:
      case BOOLEAN:
      default:
        throw new UnSupportedDataTypeException(
            String.format("Unsupported data type in MaxValue: %s", seriesDataType));
    }
  }

  // columnBuilder should be single in countAccumulator
  @Override
  public void outputIntermediate(ColumnBuilder[] columnBuilders) {
    checkArgument(columnBuilders.length == 1, "partialResult of MaxValue should be 1");
    if (!initResult) {
      columnBuilders[0].appendNull();
      return;
    }
    switch (seriesDataType) {
      case INT32:
        columnBuilders[0].writeInt(maxResult.getInt());
        break;
      case INT64:
        columnBuilders[0].writeLong(maxResult.getLong());
        break;
      case FLOAT:
        columnBuilders[0].writeFloat(maxResult.getFloat());
        break;
      case DOUBLE:
        columnBuilders[0].writeDouble(maxResult.getDouble());
        break;
      case TEXT:
      case BOOLEAN:
      default:
        throw new UnSupportedDataTypeException(
            String.format("Unsupported data type in MaxValue: %s", seriesDataType));
    }
  }

  @Override
  public void outputFinal(ColumnBuilder columnBuilder) {
    if (!initResult) {
      columnBuilder.appendNull();
      return;
    }
    switch (seriesDataType) {
      case INT32:
        columnBuilder.writeInt(maxResult.getInt());
        break;
      case INT64:
        columnBuilder.writeLong(maxResult.getLong());
        break;
      case FLOAT:
        columnBuilder.writeFloat(maxResult.getFloat());
        break;
      case DOUBLE:
        columnBuilder.writeDouble(maxResult.getDouble());
        break;
      case TEXT:
      case BOOLEAN:
      default:
        throw new UnSupportedDataTypeException(
            String.format("Unsupported data type in MaxValue: %s", seriesDataType));
    }
  }

  @Override
  public void reset() {
    initResult = false;
    this.maxResult.reset();
  }

  @Override
  public boolean hasFinalResult() {
    return false;
  }

  @Override
  public TSDataType[] getIntermediateType() {
    return new TSDataType[] {maxResult.getDataType()};
  }

  @Override
  public TSDataType getFinalType() {
    return maxResult.getDataType();
  }

  private int addIntInput(Column[] column, IWindow curWindow, boolean ignoringNull) {
    int curPositionCount = column[0].getPositionCount();

    for (int i = 0; i < curPositionCount; i++) {
      // skip null value in control column
      if (ignoringNull && column[0].isNull(i)) {
        continue;
      }
      if (!curWindow.satisfy(column[0], i)) {
        return i;
      }
      curWindow.mergeOnePoint(column, i);
      if (!column[2].isNull(i)) {
        updateIntResult(column[2].getInt(i));
      }
    }
    return curPositionCount;
  }

  private void updateIntResult(int maxVal) {
    if (!initResult || maxVal > maxResult.getInt()) {
      initResult = true;
      maxResult.setInt(maxVal);
    }
  }

  private int addLongInput(Column[] column, IWindow curWindow, boolean ignoringNull) {
    int curPositionCount = column[0].getPositionCount();

    for (int i = 0; i < curPositionCount; i++) {
      // skip null value in control column
      if (ignoringNull && column[0].isNull(i)) {
        continue;
      }
      if (!curWindow.satisfy(column[0], i)) {
        return i;
      }
      curWindow.mergeOnePoint(column, i);
      if (!column[2].isNull(i)) {
        updateLongResult(column[2].getLong(i));
      }
    }
    return curPositionCount;
  }

  private void updateLongResult(long maxVal) {
    if (!initResult || maxVal > maxResult.getLong()) {
      initResult = true;
      maxResult.setLong(maxVal);
    }
  }

  private int addFloatInput(Column[] column, IWindow curWindow, boolean ignoringNull) {
    int curPositionCount = column[0].getPositionCount();

    for (int i = 0; i < curPositionCount; i++) {
      // skip null value in control column
      if (ignoringNull && column[0].isNull(i)) {
        continue;
      }
      if (!curWindow.satisfy(column[0], i)) {
        return i;
      }
      curWindow.mergeOnePoint(column, i);
      if (!column[2].isNull(i)) {
        updateFloatResult(column[2].getFloat(i));
      }
    }
    return curPositionCount;
  }

  private void updateFloatResult(float maxVal) {
    if (!initResult || maxVal > maxResult.getFloat()) {
      initResult = true;
      maxResult.setFloat(maxVal);
    }
  }

  private int addDoubleInput(Column[] column, IWindow curWindow, boolean ignoringNull) {
    int curPositionCount = column[0].getPositionCount();

    for (int i = 0; i < curPositionCount; i++) {
      // skip null value in control column
      if (ignoringNull && column[0].isNull(i)) {
        continue;
      }
      if (!curWindow.satisfy(column[0], i)) {
        return i;
      }
      curWindow.mergeOnePoint(column, i);
      if (!column[2].isNull(i)) {
        updateDoubleResult(column[2].getDouble(i));
      }
    }
    return curPositionCount;
  }

  private void updateDoubleResult(double maxVal) {
    if (!initResult || maxVal > maxResult.getDouble()) {
      initResult = true;
      maxResult.setDouble(maxVal);
    }
  }
}
