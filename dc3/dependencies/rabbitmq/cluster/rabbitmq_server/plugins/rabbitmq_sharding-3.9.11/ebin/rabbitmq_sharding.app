{application, 'rabbitmq_sharding', [
	{description, "RabbitMQ Sharding Plugin"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['rabbit_sharding_exchange_decorator','rabbit_sharding_exchange_type_modulus_hash','rabbit_sharding_interceptor','rabbit_sharding_policy_validator','rabbit_sharding_shard','rabbit_sharding_util']},
	{registered, []},
	{applications, [kernel,stdlib,rabbit_common,rabbit]},
	{env, []},
		{broker_version_requirements, []}
]}.