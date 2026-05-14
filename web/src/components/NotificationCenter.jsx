import React, { useEffect, useRef, useState } from 'react';

import { DEFAULT_API_BASE, getWsBaseFromHttpApi } from "../config";

const NotificationCenter = () => {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [id] = useState(() => Date.now());
  const wsRef = useRef(null);
  
  useEffect(() => {  
    const wsRoot = getWsBaseFromHttpApi(DEFAULT_API_BASE);
    const websocket = new WebSocket(`${wsRoot}/ws/${id}`);
    wsRef.current = websocket;

    websocket.onopen = () => console.log('Connected to WebSocket server');
    websocket.onmessage = (event) => {
      const msg = JSON.parse(event.data)      
      console.log(msg);
      setMessages((prevMessages) => [...prevMessages, msg["msg"] ? msg["msg"] : Object.values(msg)[0]]);
    };
    websocket.onclose = () => console.log('Disconnected from WebSocket server');

    // Cleanup on unmount
    return () => websocket.close();
  }, [id]);

  const sendMessage = () => {
    const ws = wsRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(input);
      setInput('');
    }

  };
  return (
    <div className="notification-center">
      <h2>Real-Time Notifications</h2>
      <span>{id || "ndad"}</span>
      <div className="messages">
        {messages.map((message, index) => (
          <p key={index}>{message}</p>
        ))}
      </div>
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder="Type a message"
      />
      <button onClick={sendMessage}>Send</button>
    </div>
  );
};

export default NotificationCenter;