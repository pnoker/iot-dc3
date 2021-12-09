{application, 'rabbitmq_auth_backend_http', [
	{description, "RabbitMQ HTTP Authentication Backend"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['rabbit_auth_backend_http','rabbit_auth_backend_http_app']},
	{registered, [rabbitmq_auth_backend_http_sup]},
	{applications, [kernel,stdlib,inets,rabbit_common,rabbit,amqp_client]},
	{mod, {rabbit_auth_backend_http_app, []}},
	{env, [
	    {http_method,   get},
	    {user_path,     "http://localhost:8000/auth/user"},
	    {vhost_path,    "http://localhost:8000/auth/vhost"},
	    {resource_path, "http://localhost:8000/auth/resource"},
	    {topic_path,    "http://localhost:8000/auth/topic"}
	  ]},
		{broker_version_requirements, []}
]}.