/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.sparql.core;

import static org.apache.jena.sparql.util.graph.GraphUtils.triples2quads ;
import static org.apache.jena.sparql.util.graph.GraphUtils.triples2quadsDftGraph ;

import java.util.HashMap ;
import java.util.Iterator ;
import java.util.Map ;

import org.apache.jena.atlas.iterator.IteratorConcat ;
import org.apache.jena.graph.Graph ;
import org.apache.jena.graph.Node ;
import org.apache.jena.graph.Triple ;
import org.apache.jena.sparql.ARQException ;
import org.apache.jena.sparql.core.DatasetGraphFactory.GraphMaker ;

/** Implementation of a DatasetGraph as an extensible set of graphs.
 *  Subclasses need to manage any implicit graph creation.
 *  This implementation provides copy-in, copy-out for {@link #addGraph}  
 */
public class DatasetGraphMap extends DatasetGraphTriplesQuads implements TransactionalNotSupported
{
    private final GraphMaker graphMaker ;
    private final Map<Node, Graph> graphs = new HashMap<>() ;

    private Graph defaultGraph ;
    
    public DatasetGraphMap() {
        this(DatasetGraphFactory.memGraphMaker) ; 
    }
    
    public DatasetGraphMap(GraphMaker graphMaker) {
        this(graphMaker.create(), graphMaker) ;
    }
    
    public DatasetGraphMap(Graph defaultGraph, GraphMaker graphMaker) {
        this.defaultGraph = defaultGraph ;
        this.graphMaker = graphMaker ;
    }
    
    @Override
    public Iterator<Node> listGraphNodes() {
        return graphs.keySet().iterator();
    }

    @Override
    protected void addToDftGraph(Node s, Node p, Node o) {
        getDefaultGraph().add(Triple.create(s, p, o)) ;
    }

    @Override
    protected void addToNamedGraph(Node g, Node s, Node p, Node o) {
        getGraph(g).add(Triple.create(s, p, o)) ;
    }

    @Override
    protected void deleteFromDftGraph(Node s, Node p, Node o) {
        getDefaultGraph().delete(Triple.create(s, p, o)) ;
    }

    @Override
    protected void deleteFromNamedGraph(Node g, Node s, Node p, Node o) {
        getGraph(g).delete(Triple.create(s, p, o)) ;
    }

    @Override
    protected Iterator<Quad> findInDftGraph(Node s, Node p, Node o) {
        Iterator<Triple> iter = getDefaultGraph().find(s, p, o) ;
        return triples2quadsDftGraph(iter)  ;
        
    }

    @Override
    protected Iterator<Quad> findInSpecificNamedGraph(Node g, Node s, Node p, Node o) {
        Iterator<Triple> iter = getGraph(g).find(s, p, o) ;
        return triples2quads(g, iter) ;
    }

    @Override
    protected Iterator<Quad> findInAnyNamedGraphs(Node s, Node p, Node o) {
        Iterator<Node> gnames = listGraphNodes() ;
        IteratorConcat<Quad> iter = new IteratorConcat<>() ;

        // Named graphs
        for ( ; gnames.hasNext() ; )  
        {
            Node gn = gnames.next();
            Iterator<Quad> qIter = findInSpecificNamedGraph(gn, s, p, o) ;
            if ( qIter != null )
                iter.add(qIter) ;
        }
        return iter ;
    }

    @Override
    public Graph getDefaultGraph() {
        return defaultGraph;
    }

    @Override
    public Graph getGraph(Node graphNode) {
        Graph g = graphs.get(graphNode);
        if ( g == null ) {
            g = getGraphCreate();
            if ( g != null )
                graphs.put(graphNode, g);
        }
        return g;
    }

    /** Called from getGraph when a nonexistent graph is asked for.
     * Return null for "nothing created as a graph"
     */
    protected Graph getGraphCreate() { 
        Graph g = graphMaker.create() ;
        if ( g == null )
            throw new ARQException("Can't make new graphs") ;
        return g ;
    }
    
    @Override
    public long size() {
        return graphs.size();
    }
}
