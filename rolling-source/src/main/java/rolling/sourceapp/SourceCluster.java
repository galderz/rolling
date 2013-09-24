package rolling.sourceapp;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Source cluster demo, start with:
 * -Dcom.sun.management.jmxremote
 * -Dcom.sun.management.jmxremote.port=2626
 * -Dcom.sun.management.jmxremote.authenticate=false
 * -Dcom.sun.management.jmxremote.ssl=false
 *
 * Plus, if running locally:
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1
 *
 * @author Galder Zamarreño
 * @since // TODO
 */
public class SourceCluster {

   static final Log log = LogFactory.getLog(SourceCluster.class);

   static Cache<Integer, String> c1;
   static Cache<Integer, String> c2;

   public static void main(String[] args) throws Exception {
      EmbeddedCacheManager cm1 = createCacheManager("Source-CacheManager-1");
      EmbeddedCacheManager cm2 = createCacheManager("Source-CacheManager-2");
      waitForClusterToForm(cm1, cm2);
      c1 = cm1.getCache();
      c2 = cm1.getCache();
      int i = 0;

      // Initial data
      put(++i, "v" + i);
      put(++i, "v" + i);
      put(++i, "v" + i);

      System.out.println("Press <ENTER> to store more data...");
      while(true) {
         System.in.read();
         put(++i, "v" + i);
      }
   }

   static void put(Integer key, String value) {
      c1.put(key, value);
      assertEquals(value, c2.get(key));
      log.infof("Stored %s=%s", key, value);
   }

   static EmbeddedCacheManager createCacheManager(String cacheManagerName) {
      GlobalConfigurationBuilder globalBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      globalBuilder
            .globalJmxStatistics().cacheManagerName(cacheManagerName);

      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder
            .clustering().cacheMode(CacheMode.REPL_SYNC)
            .jmxStatistics().enable();

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
