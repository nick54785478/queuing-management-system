import os, glob

base_dir = r'd:\桌面\MonoRepository\queuing-system\queuing-service\src'
java_files = glob.glob(base_dir + '/**/*.java', recursive=True)

for file in java_files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    if file.endswith('Command.java') and r'application\ticket\command' in file:
        content = content.replace('package com.example.qms.application.ticket.dto;', 'package com.example.qms.application.ticket.command;')
    elif file.endswith('Query.java') and r'application\ticket\query' in file:
        content = content.replace('package com.example.qms.application.ticket.dto;', 'package com.example.qms.application.ticket.query;')
    
    content = content.replace('import com.example.qms.application.ticket.dto.DequeueTicketCommand;', 'import com.example.qms.application.ticket.command.DequeueTicketCommand;')
    content = content.replace('import com.example.qms.application.ticket.dto.EnqueueTicketCommand;', 'import com.example.qms.application.ticket.command.EnqueueTicketCommand;')
    content = content.replace('import com.example.qms.application.ticket.dto.PingTicketHeartbeatCommand;', 'import com.example.qms.application.ticket.command.PingTicketHeartbeatCommand;')
    content = content.replace('import com.example.qms.application.ticket.dto.SyncTicketProgressQuery;', 'import com.example.qms.application.ticket.query.SyncTicketProgressQuery;')
    
    if content != original:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(content)
