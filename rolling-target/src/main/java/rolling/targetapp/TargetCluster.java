package rolling.targetapp;

import org.infinispan.Cache;
import org.infinispan.cli.Context;
import org.infinispan.cli.commands.Command;
import org.infinispan.cli.commands.ProcessedCommand;
import org.infinispan.cli.connection.Connection;
import org.infinispan.cli.connection.ConnectionFactory;
import org.infinispan.cli.impl.CommandBufferImpl;
import org.infinispan.cli.impl.ContextImpl;
import org.infinispan.cli.io.StreamIOAdapter;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.cli.configuration.CLInterfaceLoaderConfigurationBuilder;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * If running locally:
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1
 *
 * @author Galder Zamarreño
 * @since // TODO
 */
public class TargetCluster {

   static final Log log = LogFactory.getLog(TargetCluster.class);

   static final String SOURCE_CONNECTION_STRING = "jmx://localhost:2626/Source-CacheManager-1/___defaultcache";
   static final String TARGET_CONNECTION_STRING = "jmx://localhost:3636/Target-CacheManager-1/___defaultcache";

   static Cache<Integer, String> c1;
   static Cache<Integer, String> c2;

   public static void main(String[] args) throws Exception {
      EmbeddedCacheManager cm1 = createCacheManager("Target-CacheManager-1");
      EmbeddedCacheManager cm2 = createCacheManager("Target-CacheManager-2");
      waitForClusterToForm(cm1, cm2);
      c1 = cm1.getCache();
      c2 = cm1.getCache();

      // Connect via CLI to the source cluster and record known key set
      recordKnownKeySet();

      // Connect via the CLI to the target cluster and synchronize data
      synchronizeData();

      int i = 0;

      System.out.println("Press <ENTER> to retrieve more data...");
      while(true) {
         System.in.read();
         get(++i, "v" + i);
      }
   }

   private static void recordKnownKeySet() throws Exception {
      Context ctx = connectCLInterface(SOURCE_CONNECTION_STRING);
      ProcessedCommand parsed = new ProcessedCommand("upgrade --dumpKeys;");
      Command command = ctx.getCommandRegistry().getCommand(parsed.getCommand());
      command.execute(ctx, parsed);
   }

   private static void synchronizeData() throws Exception {
      Context ctx = connectCLInterface(TARGET_CONNECTION_STRING);
      ProcessedCommand parsed = new ProcessedCommand("upgrade --synchronize=cli --all;");
      Command command = ctx.getCommandRegistry().getCommand(parsed.getCommand());
      command.execute(ctx, parsed);
   }

   private static Context connectCLInterface(String connectionString) throws Exception {
      Context ctx = new ContextImpl(new StreamIOAdapter(), new CommandBufferImpl());
      Connection connection = ConnectionFactory.getConnection(connectionString);
      connection.connect(ctx, null);
      ctx.setConnection(connection);
      return ctx;
   }

   static void get(Integer key, String value) {
      assertEquals(value, c2.get(key));
      log.infof("Verified %s=%s", key, value);
   }

   static EmbeddedCacheManager createCacheManager(String cacheManagerName) {
      GlobalConfigurationBuilder globalBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      globalBuilder.globalJmxStatistics().cacheManagerName(cacheManagerName);

      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder
         .clustering().cacheMode(CacheMode.REPL_SYNC)
         .jmxStatistics().enable()
         .persistence().addStore(CLInterfaceLoaderConfigurationBuilder.class)
            .connectionString(SOURCE_CONNECTION_STRING);

      return TestCacheManagerFactory.createClusteredCacheManager(globalBuilder, builder);
   }

   static void waitForClusterToForm(EmbeddedCacheManager... managers) {
      List<Cache<?, ?>> caches = new ArrayList<Cache<?, ?>>(managers.length);
      for (EmbeddedCacheManager manager : managers)
         caches.add(manager.getCache());

      Cache<?, ?> cache = caches.get(0);
      TestingUtil.blockUntilViewsReceived(10000, caches);
      if (cache.getCacheConfiguration().clustering().cacheMode().isClustered()) {
         TestingUtil.waitForRehashToComplete(caches);
      }
   }

}
