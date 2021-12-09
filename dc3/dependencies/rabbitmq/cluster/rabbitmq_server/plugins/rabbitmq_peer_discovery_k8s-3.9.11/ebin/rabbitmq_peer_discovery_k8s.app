{application, 'rabbitmq_peer_discovery_k8s', [
	{description, "Kubernetes-based RabbitMQ peer discovery backend"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['rabbit_peer_discovery_k8s','rabbitmq_peer_discovery_k8s','rabbitmq_peer_discovery_k8s_app','rabbitmq_peer_discovery_k8s_node_monitor','rabbitmq_peer_discovery_k8s_sup']},
	{registered, [rabbitmq_peer_discovery_k8s_sup]},
	{applications, [kernel,stdlib,rabbit_common,rabbitmq_peer_discovery_common,rabbit]},
	{mod, {rabbitmq_peer_discovery_k8s_app, []}},
	{env, []}
]}.