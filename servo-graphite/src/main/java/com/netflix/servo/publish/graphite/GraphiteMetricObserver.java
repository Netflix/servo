/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.publish.graphite;


import com.netflix.servo.Metric;
import com.netflix.servo.publish.BaseMetricObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;


/**
 * Observer that shunts metrics out to the excellent monitoring backend graphite.
 *
 * Developed and tested against version 0.9.10 of graphite.
 *
 * http://graphite.wikidot.com/
 */
public class GraphiteMetricObserver extends BaseMetricObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteMetricObserver.class);

    private final GraphiteNamingConvention namingConvention;
    private final String serverPrefix;
    private final SocketFactory socketFactory = SocketFactory.getDefault();
    private final URI graphiteServerURI;
    private Socket socket = null;

    /**
     * @param metricPrefix base name to attach onto each metric published ("metricPrefix.{rest of name}". If null
     *                     this section isn't attached. This section is useful to differentiate between multiple
     *                     nodes of the same application server in a cluster.
     * @param graphiteServerAddress address of the graphite data port in "host:port" format.
     */
    public GraphiteMetricObserver(String metricPrefix, String graphiteServerAddress )
    {
        this(metricPrefix, graphiteServerAddress, new BasicGraphiteNamingConvention());
    }

    /**
     * @param metricPrefix base name to attach onto each metric published ("metricPrefix.{rest of name}". If null
     *                     this section isn't attached. This section is useful to differentiate between multiple
     *                     nodes of the same application server in a cluster.
     * @param graphiteServerAddress address of the graphite data port in "host:port" format.
     * @param namingConvention naming convention to extract a graphite compatible name from each Metric
     */
    public GraphiteMetricObserver( String metricPrefix, String graphiteServerAddress, GraphiteNamingConvention namingConvention )
    {
        super( "GraphiteMetricObserver" + metricPrefix );
        this.namingConvention = namingConvention;
        this.serverPrefix = metricPrefix;
        this.graphiteServerURI = parseStringAsUri( graphiteServerAddress );
    }

    public void stop()
    {
        try
        {
            if ( socket != null )
            {
                socket.close();
                socket = null;
                LOGGER.info("Disconnected from graphite server: {}", graphiteServerURI);
            }
        }
        catch ( IOException e )
        {
            LOGGER.warn( "Error Stopping", e );
        }
    }

    @Override
    public void updateImpl( List<Metric> metrics )
    {
        try
        {
            if ( connectionAvailable() )
            {
                write( socket, metrics );
            }
        }
        catch ( IOException e )
        {
            LOGGER.warn( "Graphite connection failed on write", e );
        }
    }

    private boolean connectionAvailable() throws IOException
    {
        if ( socket == null || !socket.isConnected() )
        {
            if ( socket != null )
            {
                socket.close();
            }
            socket = socketFactory.createSocket(graphiteServerURI.getHost(), graphiteServerURI.getPort());
            LOGGER.info("Connected to graphite server: {}", graphiteServerURI);
        }
        return socket.isConnected();
    }

    private void write( Socket socket, Iterable<Metric> metrics ) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        int count = writeMetrics(metrics, writer);
        writer.flush();
        checkNoReturnedData(socket);

        LOGGER.debug("Wrote {} metrics to graphite", count);
    }

    private int writeMetrics(Iterable<Metric> metrics, PrintWriter writer)
    {
        int count = 0;
        for ( Metric metric : metrics )
        {
            String publishedName = namingConvention.getName(metric);

            StringBuilder sb = new StringBuilder();
            if(serverPrefix != null){
                sb.append( serverPrefix ).append( "." );
            }
            sb.append( publishedName ).append( " " );
            sb.append(metric.getValue().toString()).append(" ");
            sb.append( metric.getTimestamp() / 1000 );
            LOGGER.debug("{}", sb);
            writer.write( sb.append("\n").toString() );
            count++;
        }
        return count;
    }

    /**
     * the graphite protocol is a "one-way" streaming protocol and as such there is no easy way to check that we
     * are sending the output to the wrong place. We can aim the graphite writer at any listening socket and it
     * will never care if the data is being correctly handled. By logging any returned bytes we can help make it
     * obvious that it's talking to the wrong server/socket. In particular if its aimed at the web port of a
     * graphite server this will make it print out the HTTP error message.
     */
    private void checkNoReturnedData(Socket socket) throws IOException {
        BufferedInputStream reader = new BufferedInputStream( socket.getInputStream() );

        if ( reader.available() > 0 )
        {
            byte[] buffer = new byte[1000];
            int toRead = Math.min( reader.available(), 1000 );
            int read = reader.read( buffer, 0, toRead );
            if ( read > 0 )
            {
                LOGGER.warn( "Data returned by graphite server when expecting no response! Probably aimed at "
                        + "wrong socket or server. Make sure you are publishing to the data port, not the dashboard port. " +
                        "First {} bytes of response: {}", read, new String( buffer, 0, read, "UTF-8" ) );
            }
        }
    }

    /**
     * It's alot easier to configure and manage the location of the graphite server if we combine the ip and port into a
     * single string. Using a "fake" transport and the ipString means we get standard host/port parsing (including domain
     * names, ipv4 and ipv6) for free.
     *
     * Based on http://stackoverflow.com/questions/2345063/java-common-way-to-validate-and-convert-hostport-to-inetsocketaddress
     */
    private static URI parseStringAsUri( String ipString )
    {
        try
        {
            URI uri = new URI( "socket://" + ipString );
            if ( uri.getHost() == null || uri.getPort() == -1 )
            {
                throw new URISyntaxException( ipString, "URI must have host and port parts" );
            }
            return uri;
        }
        catch ( URISyntaxException e )
        {
            throw new IllegalArgumentException( "Graphite server address needs to be defined as {host}:{port}." );
        }
    }
}
