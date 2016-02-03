package com.leidos.lanemerge.config;

import com.leidos.lanemerge.services.LaneMergeService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Wrapper to always return a reference to the Spring Application Context from
 * within non-Spring enabled beans. Unlike Spring MVC's WebApplicationContextUtils
 * we do not need a reference to the Servlet context for this. All we need is
 * for this bean to be initialized during application startup.
 */
public class LaneMergeApplicationContext implements ApplicationContextAware {

    private static ApplicationContext CONTEXT;
    LaneMergeService service;

    private LaneMergeApplicationContext() {
    }

    private static class LaneMergeApplicationContextHolder  {
        private static final LaneMergeApplicationContext _instance = new LaneMergeApplicationContext();
    }

    public static LaneMergeApplicationContext getInstance()
    {
        return LaneMergeApplicationContextHolder._instance;
    }


    /**
     * This method is called from within the SpeedControl once it is
     * done starting up, it will stick a reference to the app context into this bean.
     * @param context a reference to the ApplicationContext.
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        CONTEXT = context;
    }

    /**
     * get the ApplicationContext
     *
     * @return
     */
    public ApplicationContext getApplicationContext()   {
        return CONTEXT;
    }

    /**
     * Provide access to spring beans through non-spring managed classes
     *
     * @param tClass
     * @param <T>
     * @return
     * @throws org.springframework.beans.BeansException
     */
    public <T>  T getBean(Class<T> tClass) throws BeansException   {
        return CONTEXT.getBean(tClass);
    }

    /**
     * Directly acquire AppConfig
     *
     * @return
     */
    public AppConfig getAppConfig()   {

        //CAUTION: debugging w/IntelliJ shows an exception being thrown by this call, but everything seems to function correctly.
        AppConfig res = getBean(AppConfig.class);
        return res;
    }

    public void setService(LaneMergeService service)   {
        this.service = service;
    }

    public LaneMergeService getService()  {
        return service;
    }
}