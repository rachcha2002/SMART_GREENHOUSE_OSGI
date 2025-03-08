package com.greenhouse.pest.servicepublisher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private ServiceRegistration<?> registration;
    private PestServicePublishImpl pestService;

    @Override
    public void start(BundleContext bundleContext) {
        pestService = new PestServicePublishImpl();
        pestService.start();
        registration = bundleContext.registerService(PestServicePublish.class, pestService, null);
        System.out.println("[PestDetectionCamera] Service Registered.");
    }

    @Override
    public void stop(BundleContext bundleContext) {
        if (pestService != null) {
            pestService.stop();
        }
        if (registration != null) {
            registration.unregister();
        }
        System.out.println("[PestDetectionCamera] Service Stopped.");
    }
}
