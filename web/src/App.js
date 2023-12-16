import React from 'react';
import './App.css';

function App() {
  return (
      <div className="App">
        <div className="chat-container">
          <div className="thread-list">
            {/* Example threads */}
            <div className="thread">Thread 1</div>
            <div className="thread">Thread 2</div>
            <div className="thread">Thread 3</div>
            {/* More threads */}
          </div>
          <div className="chat-window">
            <div className="messages">
              {/* Chat messages will be displayed here */}
            </div>
            <div className="chat-input">
              <input type="text" placeholder="Type a message..." />
            </div>
          </div>
        </div>
      </div>
  );
}

export default App;