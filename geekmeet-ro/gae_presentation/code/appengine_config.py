import gae_mini_profiler.profiler


appstats_RECORD_FRACTION = 1.0


def webapp_add_wsgi_middleware(app):
    return gae_mini_profiler.profiler.ProfilerWSGIMiddleware(app)

