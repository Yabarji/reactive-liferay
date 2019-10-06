package it.cb.reactive.internal.http.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ServiceTrackerProcessor<T1, T2> {

	private ServiceTrackerProcessor(
		BundleContext bundleContext, Class<T1> clazz,
		Function<BundleContext, Function<ServiceReference<T1>, T2>> as,
		Function<BundleContext, BiConsumer<ServiceReference<T1> , T2>> rs) {

		_bundleContext = bundleContext;
		_clazz = clazz;
		_onAddingService = as;
		_onRemovedService = rs;

	}

	private ServiceTrackerProcessor(
		BundleContext bundleContext, Class<T1> clazz) {

		_bundleContext = bundleContext;
		_clazz = clazz;
		_onAddingService = b -> s -> (T2)b.getService(s);
		_onRemovedService = b -> (r, t2) -> b.ungetService(r);

	}

	public <T3> ServiceTrackerProcessor<T1, T3> map(
		Function<BundleContext, Function<ServiceReference<T1>, T3>>
			addingService,
		Function<BundleContext, BiConsumer<ServiceReference<T1> , T3>>
			removedService) {
		return new ServiceTrackerProcessor<>(
			_bundleContext, _clazz, addingService, removedService);
	}


	public ServiceTracker<T1, T2> open() {

		return ServiceTrackerUtil.open(
			_bundleContext, _clazz, new ServiceTrackerCustomizer<T1, T2>() {
				@Override
				public T2 addingService(ServiceReference<T1> reference) {
					return _onAddingService
						.apply(_bundleContext)
						.apply(reference);
				}

				@Override
				public void modifiedService(
					ServiceReference<T1> reference, T2 service) {

					removedService(reference, service);

					addingService(reference);

				}

				@Override
				public void removedService(
					ServiceReference<T1> reference, T2 service) {
					_onRemovedService
						.apply(_bundleContext)
						.accept(reference, service);
				}
			});

	}

	public static <T1> ServiceTrackerProcessor<T1, T1> create(
		BundleContext context, Class<T1> clazz) {
		return new ServiceTrackerProcessor<>(context, clazz);
	}

	private final BundleContext _bundleContext;

	private final Class<T1> _clazz;

	private final Function<BundleContext, Function<ServiceReference<T1>, T2>>
		_onAddingService;

	private final Function<BundleContext, BiConsumer<ServiceReference<T1>, T2>>
		_onRemovedService;

}
