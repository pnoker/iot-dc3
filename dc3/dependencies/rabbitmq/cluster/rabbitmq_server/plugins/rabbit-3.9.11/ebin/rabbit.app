{application, 'rabbit', [
	{description, "RabbitMQ"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['amqqueue','amqqueue_v1','background_gc','code_server_cache','gatherer','gm','internal_user','internal_user_v1','lqueue','mirrored_supervisor_sups','pg_local','pid_recomposition','rabbit','rabbit_access_control','rabbit_alarm','rabbit_amqqueue','rabbit_amqqueue_process','rabbit_amqqueue_sup','rabbit_amqqueue_sup_sup','rabbit_auth_backend_internal','rabbit_auth_mechanism_amqplain','rabbit_auth_mechanism_cr_demo','rabbit_auth_mechanism_plain','rabbit_autoheal','rabbit_backing_queue','rabbit_basic','rabbit_binding','rabbit_boot_steps','rabbit_channel','rabbit_channel_interceptor','rabbit_channel_sup','rabbit_channel_sup_sup','rabbit_channel_tracking','rabbit_channel_tracking_handler','rabbit_classic_queue','rabbit_client_sup','rabbit_config','rabbit_confirms','rabbit_connection_helper_sup','rabbit_connection_sup','rabbit_connection_tracking','rabbit_connection_tracking_handler','rabbit_control_pbe','rabbit_core_ff','rabbit_core_metrics_gc','rabbit_credential_validation','rabbit_credential_validator','rabbit_credential_validator_accept_everything','rabbit_credential_validator_min_password_length','rabbit_credential_validator_password_regexp','rabbit_dead_letter','rabbit_definitions','rabbit_definitions_import_https','rabbit_definitions_import_local_filesystem','rabbit_diagnostics','rabbit_direct','rabbit_direct_reply_to','rabbit_disk_monitor','rabbit_epmd_monitor','rabbit_event_consumer','rabbit_exchange','rabbit_exchange_decorator','rabbit_exchange_parameters','rabbit_exchange_type_direct','rabbit_exchange_type_fanout','rabbit_exchange_type_headers','rabbit_exchange_type_invalid','rabbit_exchange_type_topic','rabbit_feature_flags','rabbit_ff_extra','rabbit_ff_registry','rabbit_fhc_helpers','rabbit_fifo','rabbit_fifo_client','rabbit_fifo_index','rabbit_fifo_v0','rabbit_file','rabbit_framing','rabbit_global_counters','rabbit_guid','rabbit_health_check','rabbit_limiter','rabbit_log_channel','rabbit_log_connection','rabbit_log_feature_flags','rabbit_log_mirroring','rabbit_log_prelaunch','rabbit_log_queue','rabbit_log_tail','rabbit_log_upgrade','rabbit_logger_exchange_h','rabbit_looking_glass','rabbit_maintenance','rabbit_memory_monitor','rabbit_metrics','rabbit_mirror_queue_coordinator','rabbit_mirror_queue_master','rabbit_mirror_queue_misc','rabbit_mirror_queue_mode','rabbit_mirror_queue_mode_all','rabbit_mirror_queue_mode_exactly','rabbit_mirror_queue_mode_nodes','rabbit_mirror_queue_slave','rabbit_mirror_queue_sync','rabbit_mnesia','rabbit_mnesia_rename','rabbit_msg_file','rabbit_msg_record','rabbit_msg_store','rabbit_msg_store_ets_index','rabbit_msg_store_gc','rabbit_networking','rabbit_node_monitor','rabbit_nodes','rabbit_osiris_metrics','rabbit_parameter_validation','rabbit_password','rabbit_password_hashing_md5','rabbit_password_hashing_sha256','rabbit_password_hashing_sha512','rabbit_peer_discovery','rabbit_peer_discovery_classic_config','rabbit_peer_discovery_dns','rabbit_plugins','rabbit_policies','rabbit_policy','rabbit_policy_merge_strategy','rabbit_prelaunch_cluster','rabbit_prelaunch_enabled_plugins_file','rabbit_prelaunch_feature_flags','rabbit_prelaunch_logging','rabbit_prequeue','rabbit_priority_queue','rabbit_queue_consumers','rabbit_queue_decorator','rabbit_queue_index','rabbit_queue_location_client_local','rabbit_queue_location_min_masters','rabbit_queue_location_random','rabbit_queue_location_validator','rabbit_queue_master_location_misc','rabbit_queue_master_locator','rabbit_queue_type','rabbit_queue_type_util','rabbit_quorum_memory_manager','rabbit_quorum_queue','rabbit_ra_registry','rabbit_reader','rabbit_recovery_terms','rabbit_restartable_sup','rabbit_router','rabbit_runtime_parameters','rabbit_ssl','rabbit_stream_coordinator','rabbit_stream_queue','rabbit_sup','rabbit_sysmon_handler','rabbit_sysmon_minder','rabbit_table','rabbit_trace','rabbit_tracking','rabbit_upgrade','rabbit_upgrade_functions','rabbit_upgrade_preparation','rabbit_variable_queue','rabbit_version','rabbit_vhost','rabbit_vhost_limit','rabbit_vhost_msg_store','rabbit_vhost_process','rabbit_vhost_sup','rabbit_vhost_sup_sup','rabbit_vhost_sup_wrapper','rabbit_vm','supervised_lifecycle','tcp_listener','tcp_listener_sup','term_to_binary_compat','vhost','vhost_v1']},
	{registered, [rabbit_sup,rabbit_amqqueue_sup,rabbit_direct_client_sup,rabbit_log,rabbit_node_monitor,rabbit_router]},
	{applications, [kernel,stdlib,sasl,rabbitmq_prelaunch,os_mon,inets,compiler,public_key,crypto,ssl,syntax_tools,xmerl,ranch,rabbit_common,ra,sysmon_handler,stdout_formatter,recon,observer_cli,osiris,amqp10_common,syslog,systemd,seshat]},
	{mod, {rabbit, []}},
	{env, [
	    {tcp_listeners, [5672]},
	    {num_tcp_acceptors, 10},
	    {ssl_listeners, []},
	    {num_ssl_acceptors, 10},
	    {ssl_options, []},
	    {vm_memory_high_watermark, 0.4},
	    {vm_memory_high_watermark_paging_ratio, 0.5},
	    {vm_memory_calculation_strategy, rss},
	    {memory_monitor_interval, 2500},
	    {disk_free_limit, 50000000}, %% 50MB
	    {msg_store_index_module, rabbit_msg_store_ets_index},
	    {backing_queue_module, rabbit_variable_queue},
	    %% 0 ("no limit") would make a better default, but that
	    %% breaks the QPid Java client
	    {frame_max, 131072},
	    %% see rabbitmq-server#1593
	    {channel_max, 2047},
	    {connection_max, infinity},
	    {heartbeat, 60},
	    {msg_store_file_size_limit, 16777216},
	    {msg_store_shutdown_timeout, 600000},
	    {fhc_write_buffering, true},
	    {fhc_read_buffering, false},
	    {queue_index_max_journal_entries, 32768},
	    {queue_index_embed_msgs_below, 4096},
	    {default_user, <<"guest">>},
	    {default_pass, <<"guest">>},
	    {default_user_tags, [administrator]},
	    {default_vhost, <<"/">>},
	    {default_permissions, [<<".*">>, <<".*">>, <<".*">>]},
	    {loopback_users, [<<"guest">>]},
	    {password_hashing_module, rabbit_password_hashing_sha256},
	    {server_properties, []},
	    {collect_statistics, none},
	    {collect_statistics_interval, 5000},
	    {mnesia_table_loading_retry_timeout, 30000},
	    {mnesia_table_loading_retry_limit, 10},
	    {auth_mechanisms, ['PLAIN', 'AMQPLAIN']},
	    {auth_backends, [rabbit_auth_backend_internal]},
	    {delegate_count, 16},
	    {trace_vhosts, []},
	    {ssl_cert_login_from, distinguished_name},
	    {ssl_handshake_timeout, 5000},
	    {ssl_allow_poodle_attack, false},
	    {handshake_timeout, 10000},
	    {reverse_dns_lookups, false},
	    {cluster_partition_handling, ignore},
	    {cluster_keepalive_interval, 10000},
	    {autoheal_state_transition_timeout, 60000},
	    {tcp_listen_options, [{backlog,       128},
	                          {nodelay,       true},
	                          {linger,        {true, 0}},
	                          {exit_on_close, false}
	                         ]},
	    {halt_on_upgrade_failure, true},
	    {ssl_apps, [asn1, crypto, public_key, ssl]},
	    %% see rabbitmq-server#114
	    {mirroring_flow_control, true},
	    {mirroring_sync_batch_size, 4096},
	    %% see rabbitmq-server#227 and related tickets.
	    %% msg_store_credit_disc_bound only takes effect when
	    %% messages are persisted to the message store. If messages
	    %% are embedded on the queue index, then modifying this
	    %% setting has no effect because credit_flow is not used when
	    %% writing to the queue index. See the setting
	    %% queue_index_embed_msgs_below above.
	    {msg_store_credit_disc_bound, {4000, 800}},
	    {msg_store_io_batch_size, 4096},
	    %% see rabbitmq-server#143,
	    %% rabbitmq-server#949, rabbitmq-server#1098
	    {credit_flow_default_credit, {400, 200}},
	    {quorum_commands_soft_limit, 32},
	    {quorum_cluster_size, 3},
	    %% see rabbitmq-server#248
	    %% and rabbitmq-server#667
	    {channel_operation_timeout, 15000},
	    %% See https://www.rabbitmq.com/consumers.html#acknowledgement-timeout
	    %% 30 minutes
	    {consumer_timeout, 1800000},

	    %% see rabbitmq-server#486
	    {autocluster,
              [{peer_discovery_backend, rabbit_peer_discovery_classic_config}]
            },
	    %% used by rabbit_peer_discovery_classic_config
	    {cluster_nodes, {[], disc}},

	    {config_entry_decoder, [{passphrase, undefined}]},

	    %% rabbitmq-server#973
	    {queue_explicit_gc_run_operation_threshold, 1000},
	    {lazy_queue_explicit_gc_run_operation_threshold, 1000},
	    {background_gc_enabled, false},
	    {background_gc_target_interval, 60000},
	    %% rabbitmq-server#589
	    {proxy_protocol, false},
	    {disk_monitor_failure_retries, 10},
	    {disk_monitor_failure_retry_interval, 120000},
	    %% either "stop_node" or "continue".
	    %% by default we choose to not terminate the entire node if one
	    %% vhost had to shut down, see server#1158 and server#1280
	    {vhost_restart_strategy, continue},
	    %% {global, prefetch count}
	    {default_consumer_prefetch, {false, 0}},
		%% interval at which the channel can perform periodic actions
	    {channel_tick_interval, 60000},
	    %% Default max message size is 128 MB
	    {max_message_size, 134217728},
	    %% Socket writer will run GC every 1 GB of outgoing data
	    {writer_gc_threshold, 1000000000},
	    %% interval at which connection/channel tracking executes post operations
	    {tracking_execution_timeout, 15000},
	    {stream_messages_soft_limit, 256},
        {track_auth_attempt_source, false}
	  ]}
]}.