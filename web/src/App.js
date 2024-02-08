import React, { useEffect, useRef, useState } from 'react';
// import Modal from './Modal';
import {
    Button,
    Col,
    Flex,
    Form,
    Input,
    Layout,
    List,
    Menu,
    message,
    Modal,
    Popover,
    Row,
    Space,
    Switch
} from 'antd';
import Cookies from 'js-cookie';
import 'antd/dist/reset.css';
import {
    DeleteOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    QuestionCircleTwoTone
} from "@ant-design/icons";
import './App.css';

const { Header, Content, Sider } = Layout;
const version = 'v1.2.0';
const updateDate = '2024-02-08';

const App = () => {

    const [inputMessage, setInputMessage] = useState('');
    const [ongoingConversation, setOngoingConversation] = useState([]); // Track the ongoing conversation's messages
    const [conversations, setConversations] = useState([]); // Track the saved conversations
    const [currentConversationId, setCurrentConversationId] = useState(null); // Track the current conversation's ID
    const [isLoading, setIsLoading] = useState(false);
    const [abortController, setAbortController] = useState(null);
    const [showRegistrationModal, setShowRegistrationModal] = useState(false);
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [showLogoutModal, setShowLogoutModal] = useState(false);
    const [showUpdateModal, setShowUpdateModal] = useState(false);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [username, setUsername] = useState('');

    const [form] = Form.useForm();
    const [messageApi, contextHolder] = message.useMessage();
    // if the window width is less than 576px, the sider will be collapsed by default
    const [siderCollapsed, setSiderCollapsed] = useState(window.innerWidth < 576);
    const messagesEndRef = useRef(null);

    // features
    const [enabledPromptOptimization, setEnabledPromptOptimization] = useState(false);

    let API_URL = 'https://simons-gpt.azurewebsites.net';
    // let API_URL = 'http://localhost:8080';

    // eslint-disable-next-line
    const infoMessage = (message) => {
        messageApi.info(message).then();
    };

    const successMessage = (message) => {
        messageApi.open({
            type: 'success',
            content: message,
        }).then();
    };

    const errorMessage = (message) => {
        messageApi.open({
            type: 'error',
            content: message,
        }).then();
    };

    // eslint-disable-next-line
    const warningMessage = (message) => {
        messageApi.open({
            type: 'warning',
            content: message,
        }).then();
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const handleInputChange = (event) => {
        setInputMessage(event.target.value);
    };

    const sendMessage = async () => {
        if (inputMessage.trim()) {
            const controller = new AbortController();
            setAbortController(controller);
            setIsLoading(true); // Start loading

            // if this is the first message of a new conversation, create one in the backend
            if (isAuthenticated && !currentConversationId) {
                try {
                    const conversation = await createConversation();
                    await fetchConversations();
                    setCurrentConversationId(conversation.conversationId);
                } catch (error) {
                    console.error(error);
                    errorMessage('创建对话记录失败，本条对话将不会被保存');
                }
            }
            // Add the user message to the conversation
            setOngoingConversation(prevState => [...prevState, { role: 'user', content: inputMessage }]);
            setInputMessage(''); // Clear the input field
        }
    };

    const interruptProcess = () => {
        if (abortController) {
            abortController.abort();
            setAbortController(null);
        }
        setIsLoading(false);
    }

    useEffect(() => {
        scrollToBottom();
        if (ongoingConversation.length > 0 && ongoingConversation[ongoingConversation.length - 1].role === 'user') {
            const jwtToken = Cookies.get('jwtToken');
            const headers = {
                'Content-Type': 'application/json',
            };

            if (jwtToken) {
                headers['Authorization'] = `Bearer ${jwtToken}`;
            }

            const body = {
                conversationId: currentConversationId,
                enabledPromptOptimization: enabledPromptOptimization,
                chatMessages: ongoingConversation.flatMap(function (message) {
                    return {
                        role: message.role,
                        content: message.content,
                    };
                }),
            }

            // Use the Fetch API to make a POST request
            fetch(API_URL + '/chat/messages', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(body),
                signal: abortController.signal,
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Response not OK: ' + response.status);
                    }
                    // The ReadableStream is in the response.body
                    const reader = response.body.getReader();
                    return new ReadableStream({
                        start(controller) {
                            function push() {
                                const decoder = new TextDecoder();
                                reader.read().then(({ done, value }) => {
                                    if (done) {
                                        // Finish the streaming and close the reader
                                        controller.close();
                                        setIsLoading(false); // Stop loading when done
                                        return;
                                    }
                                    // Decode the stream while handling multibyte characters
                                    const text = decoder.decode(value, { stream: true })
                                        .replaceAll('data:', '');
                                    // Add streamed assistant message to conversation
                                    setOngoingConversation(conversation => {
                                        // Append the text to the last assistant message if it exists and is the last one
                                        if (conversation.length && conversation[conversation.length - 1].role === 'assistant') {
                                            return [...conversation.slice(0, -1), {
                                                ...conversation[conversation.length - 1],
                                                content: conversation[conversation.length - 1].content + text
                                            }];
                                        } else {
                                            // Otherwise, add a new assistant message
                                            return [...conversation, { role: 'assistant', content: text }];
                                        }
                                    });
                                    controller.enqueue(value);
                                    push();
                                }).catch(error => {
                                    if (error.name === 'AbortError') {
                                        console.log('Fetch aborted');
                                    } else {
                                        // Handle other errors
                                        console.error(error);
                                        errorMessage('请求失败');
                                    }
                                    setIsLoading(false);
                                });
                            }

                            push();
                        }
                    });
                })
                .catch(error => {
                    if (error.name === 'AbortError') {
                        console.log('Fetch aborted');
                    } else {
                        // Handle other errors
                        console.error(error);
                        errorMessage('请求失败');
                    }
                    setIsLoading(false);
                });
        }
        // make sure the title is updated when the conversation changes
        fetchConversations().then();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [ongoingConversation]);

    const handleKeyPress = async (event) => {
        if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey) {
            event.preventDefault();
            await sendMessage();
        }
    };

    const fetchConversations = async () => {
        if (!isAuthenticated) return;

        const jwtToken = Cookies.get('jwt-token');
        const response = await fetch(`${API_URL}/chat/conversations`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const data = await response.json();
            setConversations(data);
        } else {
            const errorText = await response.text();
            console.error(errorText);
            errorMessage(`获取对话记录失败，请重新登录`);
            logout()
        }
    };

    const newConversation = async () => {
        setOngoingConversation([]);
        setCurrentConversationId(null);
    }

    const createConversation = async () => {
        if (!isAuthenticated) return;

        const jwtToken = Cookies.get('jwt-token');
        const response = await fetch(`${API_URL}/chat/conversations`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const newConversation = await response.json();
            setConversations([...conversations, newConversation]);
            setCurrentConversationId(newConversation.conversationId);
            return newConversation;
        } else {
            const errorText = await response.text();
            console.error(errorText);
            errorMessage(`创建对话记录失败，本条对话将不会被保存`);
        }
    };

    const switchConversation = async (conversationId) => {
        setCurrentConversationId(conversationId);
        fetchMessages(conversationId).then(messages => {
            setOngoingConversation([]);
            messages.forEach(message => {
                setOngoingConversation(prevState => [...prevState, { role: message.role, content: message.content }]);
            })
        });
    }

    const deleteConversation = async (conversationId) => {
        const jwtToken = Cookies.get('jwt-token');
        await fetch(`${API_URL}/chat/conversations/${conversationId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        })
            .then(response => {
                if (!response.ok) {
                    errorMessage(`删除对话失败：${response.text()}`)
                } else {
                    // Remove the conversation from the state
                    setConversations(conversations.filter(conversation => conversation.conversationId !== conversationId));
                    successMessage('对话已删除。');
                }
            });
    };

    const fetchMessages = async (conversationId) => {
        if (!isAuthenticated) return;

        const jwtToken = Cookies.get('jwt-token');
        const response = await fetch(`${API_URL}/chat/conversations/${conversationId}/messages`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            return await response.json();
        } else {
            const errorText = await response.text();
            errorMessage(`获取对话记录失败：${errorText}，请刷新重试`);
        }
    };

    useEffect(() => {
        const savedUsername = Cookies.get('username');
        const savedToken = Cookies.get('jwt-token');
        if (savedUsername && savedToken) {
            setUsername(savedUsername);
            setIsAuthenticated(true);
        }
        fetchConversations().then();
        // eslint-disable-next-line
    }, [isAuthenticated]);

    // Check the last visit time when the component mounts
    useEffect(() => {
        const visitedVersion = localStorage.getItem('visitedVersion');
        if (!visitedVersion || visitedVersion !== version) {
            setShowUpdateModal(true);
        }
    }, []);

    const handleVisit = () => {
        setShowUpdateModal(false);
        localStorage.setItem('visitedVersion', version);
    };

    // Function to handle user registration
    const register = async (data) => {
        await fetch(`${API_URL}/users/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        })
            .then(async response => {
                if (!response.ok) {
                    errorMessage(`注册失败：${await response.text()}`)
                } else {
                    successMessage('注册成功');
                    await login(data)
                }
            });
    };

    // Function to handle user login
    const login = async (data) => {
        await fetch(`${API_URL}/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        })
            .then(async response => {
                if (!response.ok) {
                    errorMessage(`登录失败：${await response.text()}`)
                } else {
                    const token = await response.text();
                    setUsername(data.username);
                    Cookies.set('username', data.username, { expires: 7 });
                    Cookies.set('jwt-token', token, { expires: 7 });
                    setIsAuthenticated(true);
                    successMessage('登录成功');
                }
            });
    };

    // Function to handle user logout
    const logout = () => {
        setUsername('');
        Cookies.remove('username');
        Cookies.remove('jwt-token');
        setIsAuthenticated(false);
        successMessage('注销成功')
    };

    const AuthButtons = () => {
        if (!isAuthenticated) {
            return (
                <>
                    <Space>
                        <span>登录后可以保存对话记录</span>
                        <Button
                            type="primary"
                            onClick={() => setShowRegistrationModal(true)}>
                            注册
                        </Button>
                        <Button onClick={() => setShowLoginModal(true)}>登录</Button>
                    </Space>
                </>
            );
        } else {
            return (
                <>
                    <Space>
                        <span>欢迎你，{username}</span>
                        <Button onClick={() => setShowLogoutModal(true)}>注销</Button>
                    </Space>
                </>
            );
        }
    };

    const RegistrationModal = () => {
        return (
            <Modal
                forceRender
                title="注册"
                centered
                okText="注册"
                open={showRegistrationModal}
                onCancel={() => setShowRegistrationModal(false)}
                onOk={() => {
                    form
                        .validateFields()
                        .then(values => {
                            form.resetFields();
                            setShowRegistrationModal(false);
                            register(values).then();
                        })
                        .catch(error => {
                            console.error(error);
                        });
                }}
            >
                <p style={{
                    color: 'grey',
                    fontSize: '12px',
                    lineHeight: '25px'
                }}>
                    提示：注册仅用于保存对话记录，不建议使用你在其它平台的密码，因为除了把你的真实密码加密一次之外，我的服务器没有任何安全措施。</p>
                <p style={{
                    color: 'grey',
                    fontSize: '12px',
                    lineHeight: '25px'
                }}>
                    所以你可以随便输一个，反正我也没有限制密码格式，诶嘿。
                </p>
                <Form
                    name="register"
                    form={form}
                >
                    <Form.Item
                        name="username"
                        rules={[
                            {
                                required: true,
                                message: '请输入用户名',
                            },
                        ]}
                    >
                        <Input placeholder="用户名"/>
                    </Form.Item>
                    <Form.Item
                        name="password"
                        rules={[
                            {
                                required: true,
                                message: '请输入密码',
                            },
                        ]}
                    >
                        <Input.Password placeholder="密码"/>
                    </Form.Item>
                </Form>
            </Modal>
        );
    }

    const LoginModal = () => {
        return (
            <Modal
                title="登录"
                centered
                okText="登录"
                open={showLoginModal}
                onCancel={() => setShowLoginModal(false)}
                onOk={() => {
                    form
                        .validateFields()
                        .then(values => {
                            form.resetFields();
                            setShowLoginModal(false);
                            login(values).then();
                        })
                        .catch(error => {
                            console.error(error);
                        });
                }}
            >
                <Form
                    name="login"
                    form={form}
                >
                    <Form.Item
                        name="username"
                        rules={[
                            {
                                required: true,
                                message: '请输入用户名',
                            },
                        ]}
                    >
                        <Input placeholder="用户名"/>
                    </Form.Item>
                    <Form.Item
                        name="password"
                        rules={[
                            {
                                required: true,
                                message: '请输入密码',
                            },
                        ]}
                    >
                        <Input.Password placeholder="密码"/>
                    </Form.Item>
                </Form>
            </Modal>
        );
    }

    const LogoutModal = () => {
        return (
            <Modal
                title="注销"
                centered
                okText="注销"
                open={showLogoutModal}
                onOk={() => {
                    logout();
                    setShowLogoutModal(false);
                }}
                onCancel={() => setShowLogoutModal(false)}
            >
                <p style={{
                    color: 'grey',
                    fontSize: '12px',
                    lineHeight: '25px'
                }}>
                    注销后将无法保存对话记录，但是你可以继续使用聊天功能。
                </p>
            </Modal>
        );
    }

    const UpdateModal = () => {
        return (
            <Modal
                title="欢迎来到新版本！"
                open={showUpdateModal}
                onOk={handleVisit}
                onCancel={handleVisit}
                footer={null} // Remove the default footer buttons
                // Allow the user to close the modal by clicking on the mask (outside the modal)
                maskClosable={true}
            >
                <p><i>{updateDate}</i></p>
                <p>版本更新说明：{version}</p>
                <p>1. 新增提示词优化功能，打开后会单独调用一次模型优化提示词，
                    然后再用优化过的提示词开启对话，应该可以极大提升提示词比较简单时的生成效果</p>
                <p>2. 优化了导航菜单的布局</p>
                <p>3. 修复了一些小bug</p>
                <p>Todo list：</p>
                <p>1. 集成Assistant接口，更好地支持专项领域的需求</p>
                <p>2. 给项目写个readme</p>
                <p>3. 目前生成文本时会不断调用后台接口，需要加防抖</p>
                <p>4. 重构前端代码</p>
            </Modal>
        );
    }

    const modalDeleteConversation = (conversationId) => {
        Modal.confirm({
            title: '确定要删除这段对话吗？',
            content: '本操作无法撤销。',
            onOk() {
                deleteConversation(conversationId)
                    .catch(error => {
                        console.error(error);
                    });
            },
        });
    };


    const NavigationMenu = ({ conversations, onSelectConversation }) => {
        return (
            <Menu theme={"dark"} mode="inline">
                <div style={{ padding: '0 16px' }}>
                    <Row gutter={{
                        xs: 8,
                        sm: 16,
                        md: 24,
                        lg: 32,
                    }}>
                        <Col span={24}>
                            <Flex style={{ padding: '10px' }} gap="middle" vertical>
                                <Button type="primary" block onClick={newConversation}>
                                    开启新对话
                                </Button>
                            </Flex>
                        </Col>
                    </Row>
                    <Row gutter={{
                        xs: 8,
                        sm: 16,
                        md: 24,
                        lg: 32,
                    }}>
                        <Col span={24}>
                            {isAuthenticated ?
                                conversations?.map(conversation => (
                                    <Menu.Item key={conversation.conversationId}
                                               onClick={() => onSelectConversation(conversation.conversationId)}
                                               style={{
                                                   whiteSpace: 'nowrap',
                                                   overflow: 'hidden',
                                                   textOverflow: 'ellipsis'
                                               }}
                                    >
                                        <Row align="middle">
                                            <Col flex="auto" style={{
                                                whiteSpace: 'nowrap',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                maxWidth: 'calc(100% - 40px)'

                                            }}>
                                                {conversation.title || `Conversation ${conversation.conversationId}`}
                                            </Col>
                                            <Col flex="none">
                                                <Button shape="circle"
                                                        icon={<DeleteOutlined/>}
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            modalDeleteConversation(conversation.conversationId);
                                                        }}/>
                                            </Col>
                                        </Row>
                                    </Menu.Item>
                                ))
                                : null
                            }
                        </Col>
                    </Row>
                </div>
            </Menu>
        );
    };

    return (
        <Layout hasSider style={{ minHeight: '100vh' }}>
            {contextHolder}
            <RegistrationModal/>
            <LoginModal/>
            <LogoutModal/>
            <UpdateModal/>
            <Sider
                width={window.innerWidth < 768 ? "80%" : "30%"}
                collapsible
                trigger={null}
                collapsed={siderCollapsed}
                collapsedWidth="0"
                style={{
                    overflow: 'auto',
                    height: '100vh',
                    position: 'sticky',
                    left: 0,
                    top: 0,
                    bottom: 0,
                }}>
                <NavigationMenu
                    conversations={conversations}
                    onSelectConversation={(conversationId) => switchConversation(conversationId)}
                />
            </Sider>
            <Layout>
                <Header style={{
                    background: '#fff',
                    padding: '0 16px',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    position: 'sticky',
                    top: 0,
                    zIndex: 1,
                    width: '100%',
                }}>
                    <Button
                        type="text"
                        icon={siderCollapsed ? <MenuUnfoldOutlined/> : <MenuFoldOutlined/>}
                        onClick={() => setSiderCollapsed(!siderCollapsed)}
                        style={{
                            fontSize: '12px',
                            width: 64,
                            height: 64,
                        }}
                    >
                        {siderCollapsed ? "点击展开侧栏" : "点击收起侧栏"}
                    </Button>
                    <div>
                        {/* Placeholder */}
                    </div>
                    <div>
                        <AuthButtons/>
                    </div>
                </Header>
                <Layout>
                    <Content style={{ margin: '24px 16px 0' }}>
                        <div className="chat-area" style={{ padding: 24, background: '#fff' }}>
                            {/* message area*/}
                            <Row gutter={{
                                xs: 8,
                                sm: 16,
                                md: 24,
                                lg: 32,
                            }}>
                                <Col span={24}>
                                    <div className="message-list">
                                        <List
                                            style={{ overflowY: 'auto' }}
                                            itemLayout="horizontal"
                                            dataSource={ongoingConversation}
                                            renderItem={item => (
                                                <List.Item
                                                    style={{ justifyContent: item.role === 'user' ? 'flex-end' : 'flex-start' }}>
                                                    <div
                                                        className={`message ${item.role}`}>{item.content.replaceAll(/\n\n(?!\n)/g, '')}</div>
                                                </List.Item>
                                            )}
                                        />
                                    </div>
                                    {/* Invisible element at the end of the messages */}
                                    <div ref={messagesEndRef}/>
                                </Col>
                            </Row>
                            {/* feature selecting area */}
                            <Row gutter={{
                                xs: 8,
                                sm: 16,
                                md: 24,
                                lg: 32,
                            }}>
                                <Col span={24}>
                                    <Popover
                                        trigger="click"
                                        title={"测试功能，可能不稳定"}
                                        content={
                                            <div>
                                                开启提示词优化后，后台会要求模型将你的输入进行扩充，以提供更多的上下文和更明确的指示，
                                                并用扩充后的文本替换你的原始输入；预期的回答质量会提高，但耗时会增加。<br/><br/>
                                                <b>注意：</b><br/>
                                                1. 开启这个功能会极大增加生成的文本量；<br/>
                                                2. 如果刷新后重新进入这段对话，你的首次输入会被替换成扩充后的提示词。
                                            </div>
                                        }
                                    >
                                        {<QuestionCircleTwoTone/>}
                                    </Popover>
                                    <span>提示词优化：</span>
                                    <Switch onChange={setEnabledPromptOptimization} checkedChildren="开启"
                                            unCheckedChildren="关闭"></Switch>
                                </Col>
                            </Row>
                            {/* input area*/}
                            <Row gutter={{
                                xs: 8,
                                sm: 16,
                                md: 24,
                                lg: 32,
                            }}>
                                <Col span={24}>
                                    <Space.Compact style={{ paddingTop: '10px', width: '100%' }} align="start">
                                        <Input.TextArea required
                                                        placeholder="输入你的问题……"
                                                        style={{ width: 'calc(100% - 100px)' }}
                                                        value={inputMessage}
                                                        onChange={handleInputChange}
                                                        onPressEnter={handleKeyPress}
                                                        autoSize/>
                                        {isLoading ? (
                                            <Button onClick={interruptProcess}>停止生成</Button>
                                        ) : (
                                            <Button type="primary" onClick={sendMessage}
                                                    disabled={!inputMessage.trim()}>
                                                发送
                                            </Button>
                                        )}
                                    </Space.Compact>
                                </Col>
                            </Row>
                        </div>
                    </Content>
                </Layout>
            </Layout>
        </Layout>
    );
}

export default App;
