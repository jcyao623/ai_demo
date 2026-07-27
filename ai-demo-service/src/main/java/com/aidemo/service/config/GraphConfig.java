package com.aidemo.service.config;
 
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
 import com.aidemo.service.node.*;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 
 @Configuration
 public class GraphConfig {
 
     @Bean
     public CompiledGraph wmsAllocationGraph(
             CreateOrderNode createOrderNode,
             CheckInventoryNode checkInventoryNode,
             AiDecideNode aiDecideNode,
            HumanReviewNode humanReviewNode,
            ExecuteNode executeNode,
             CompleteNode completeNode) throws GraphStateException {
         StateGraph graph = new StateGraph();
        graph.addNode("create-order", AsyncNodeAction.node_async(createOrderNode));
        graph.addNode("check-inventory", AsyncNodeAction.node_async(checkInventoryNode));
        graph.addNode("ai-decide", AsyncNodeAction.node_async(aiDecideNode));
        graph.addNode("human-review", AsyncNodeAction.node_async(humanReviewNode));
        graph.addNode("execute", AsyncNodeAction.node_async(executeNode));
        graph.addNode("complete", AsyncNodeAction.node_async(completeNode));
        graph.addEdge(StateGraph.START, "create-order");
        graph.addEdge("create-order", "check-inventory");
        graph.addEdge("check-inventory", "ai-decide");
        graph.addEdge("ai-decide",  "human-review");
        graph.addEdge("human-review", "execute");
        graph.addEdge("execute", "complete");
        graph.addEdge("complete", StateGraph.END);
         CompileConfig cc = CompileConfig.builder().interruptBefore("execute").build();
         return graph.compile(cc);
     }
 }
