//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.tset.ops;

import java.util.Iterator;
import java.util.Map;

import edu.iu.dsc.tws.api.compute.IMessage;
import edu.iu.dsc.tws.api.compute.TaskContext;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.exceptions.Twister2RuntimeException;
import edu.iu.dsc.tws.api.resource.WorkerEnvironment;
import edu.iu.dsc.tws.api.tset.TSetConstants;
import edu.iu.dsc.tws.api.tset.fn.ComputeFunc;
import edu.iu.dsc.tws.api.tset.fn.TFunction;
import edu.iu.dsc.tws.api.tset.table.Row;
import edu.iu.dsc.tws.api.tset.table.Table;
import edu.iu.dsc.tws.api.tset.table.TableBuilder;
import edu.iu.dsc.tws.api.tset.table.TableSchema;
import edu.iu.dsc.tws.tset.arrow.ArrowTableBuilder;
import edu.iu.dsc.tws.tset.arrow.TableRuntime;
import edu.iu.dsc.tws.tset.sets.BaseTSet;

public class RowComputeOp extends BaseComputeOp<Table> {
  private ComputeFunc<Row, Row> computeFunction;

  private TableBuilder builder;

  /**
   * Table max size set to 64MB
   */
  private long tableMaxSize = 64000000;

  /**
   * Table runtime to use, it contains
   */
  private TableRuntime runtime;

  /**
   * The output schema
   */
  private TableSchema schema;

  public RowComputeOp() {
  }

  public RowComputeOp(ComputeFunc<Row, Row> computeFunction, BaseTSet origin,
                   Map<String, String> receivables) {
    super(origin, receivables);
    this.computeFunction = computeFunction;
  }

  @Override
  public void prepare(Config cfg, TaskContext ctx) {
    super.prepare(cfg, ctx);
    runtime = WorkerEnvironment.getSharedValue(TableRuntime.TABLE_RUNTIME,
        TableRuntime.class);
    if (runtime == null) {
      throw new Twister2RuntimeException("Table runtime must be set");
    }

    schema = (TableSchema) ctx.getConfig(TSetConstants.OUTPUT_SCHEMA_KEY);
    tableMaxSize = cfg.getLongValue("twister2.table.max.size", tableMaxSize);
    builder = new ArrowTableBuilder(schema, runtime.getRootAllocator());
  }

  @Override
  public TFunction getFunction() {
    return this.computeFunction;
  }

  @Override
  public boolean execute(IMessage<Table> content) {
    for (Iterator<Row> it = content.getContent().getRowIterator(); it.hasNext();) {
      Row r = it.next();
      Row output = computeFunction.compute(r);

      builder.add(output);

      if (builder.currentSize() > tableMaxSize) {
        writeToEdges(builder.build());
        builder = new ArrowTableBuilder(schema, runtime.getRootAllocator());
      }
    }

    writeToEdges(builder.build());
    writeEndToEdges();
    builder = null;

    computeFunction.close();
    return true;
  }
}
