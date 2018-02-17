class ProfilerConfigProduction:
    @staticmethod
    def should_profile(environ):
        return False

class ProfilerConfigDevelopment:
    @staticmethod
    def should_profile(environ):
        return True
