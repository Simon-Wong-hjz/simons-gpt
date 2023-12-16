import React, { useState } from 'react';
import axios from 'axios';
import './App.css';

function App() {
    const [inputMessage, setInputMessage] = useState('');
    const [messages, setMessages] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [showError, setShowError] = useState(false);

    const handleInputChange = (event) => {
        setInputMessage(event.target.value);
    };

    const sendMessage = async () => {
        if (inputMessage.trim()) {
            setIsLoading(true); // Start loading
            try {
                const response = await axios.post('http://localhost:8080/chat', { message: inputMessage });
                setMessages([...messages, response.data.message]);
                setInputMessage(''); // Clear the input field
            } catch (error) {
                console.error(error)
                let errorMessage = error.message;
                if (error.response && error.response.data) {
                    errorMessage = error.response.data;
                }
                setErrorMessage('Error sending message: ' + errorMessage);
                setShowError(true);
                setTimeout(() => {
                    setShowError(false);
                }, 3000); // Error message will disappear after 3 seconds
            }
            setIsLoading(false); // Stop loading regardless of the outcome
        }
    };

    const handleKeyPress = (event) => {
        if (event.key === 'Enter') {
            sendMessage();
        }
    };

    return (
        <div className="App">
            {showError && (
                <div className="error-message">
                    {errorMessage}
                </div>
            )}
            <div className="chat-container">
                {isLoading && <div className="loading-spinner"></div>}
                <div className="chat-window">
                    <div className="messages">
                        {messages.map((msg, index) => (
                            <div key={index} className="message">
                                {msg}
                            </div>
                        ))}
                    </div>
                    <div className="chat-input">
                        <input
                            type="text"
                            placeholder="请输入你的问题..."
                            value={inputMessage}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                        />
                        <button onClick={sendMessage}>发送</button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default App;
