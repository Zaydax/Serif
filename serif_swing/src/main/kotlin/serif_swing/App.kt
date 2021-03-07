/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package xyz.room409.serif.serif_swing
import com.formdev.flatlaf.*
import xyz.room409.serif.serif_shared.*
import xyz.room409.serif.serif_shared.db.DriverFactory
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.text.*

sealed class SwingState() {
    abstract fun refresh()
}
class SwingLogin(val transition: (MatrixState, Boolean) -> Unit, val onSync: () -> Unit, val panel: JPanel, val m: MatrixLogin) : SwingState() {
    var c_left = GridBagConstraints()
    var c_right = GridBagConstraints()
    var login_message_label = JLabel(m.login_message)
    var username_field = JTextField(20)
    var username_label = JLabel("Username: ")
    var password_field = JPasswordField(20)
    var password_label = JLabel("Password: ")
    var button = JButton("Login")
    var logIn: (ActionEvent) -> Unit = { transition(m.login(username_field.text, password_field.text, onSync), true) }

    init {
        panel.layout = GridBagLayout()
        c_left.anchor = GridBagConstraints.EAST
        c_left.gridwidth = GridBagConstraints.RELATIVE
        c_left.fill = GridBagConstraints.NONE
        c_left.weightx = 0.0

        c_right.anchor = GridBagConstraints.EAST
        c_right.gridwidth = GridBagConstraints.REMAINDER
        c_right.fill = GridBagConstraints.HORIZONTAL
        c_right.weightx = 1.0

        panel.add(login_message_label, c_right)
        panel.add(JLabel("Login with previous session?"), c_right)

        for (session in m.getSessions()) {
            var button = JButton(session)
            panel.add(button, c_right)
            button.addActionListener({ transition(m.loginFromSession(session, onSync), true) })
        }

        username_label.labelFor = username_field
        panel.add(username_label, c_left)
        panel.add(username_field, c_right)

        password_label.labelFor = password_field
        panel.add(password_label, c_left)
        panel.add(password_field, c_right)

        panel.add(button, c_right)

        password_field.addActionListener(logIn)
        button.addActionListener(logIn)
    }
    override fun refresh() {
        // This should change when we have multiple sessions,
        // since it will clear all text input fields on
        // refresh
        transition(m.refresh(), true)
    }
}
class SwingRooms(val transition: (MatrixState, Boolean) -> Unit, val panel: JPanel, var m: MatrixRooms) : SwingState() {
    var message_label = JLabel(m.message)
    var inner_scroll_pane = JPanel()
    init {
        panel.layout = BorderLayout()
        panel.add(message_label, BorderLayout.PAGE_START)

        inner_scroll_pane.layout = GridLayout(0,1)
        for ((id, name, unreadCount, highlightCount, lastMessage) in m.rooms) {
            var button = JButton()
            button.layout = BoxLayout(button, BoxLayout.PAGE_AXIS)

            val room_name = JLabel("$name ($unreadCount unread / $highlightCount mentions)")
            val last_message = JLabel(lastMessage?.message?.take(80) ?: "")

            button.add(room_name)
            button.add(last_message)

            button.addActionListener({ transition(m.getRoom(id), true) })
            inner_scroll_pane.add(button)
        }
        panel.add(JScrollPane(inner_scroll_pane), BorderLayout.CENTER)

        var back_button = JButton("(Fake) Logout")
        panel.add(back_button, BorderLayout.PAGE_END)
        back_button.addActionListener({ transition(m.fake_logout(), true) })
    }
    override fun refresh() {
        transition(m.refresh(), true)
    }
    fun update(new_m: MatrixRooms) {
        if (m.rooms != new_m.rooms) {
            println("Having to transition, rooms !=")
            transition(new_m, false)
        } else {
            message_label.text = new_m.message
            m = new_m
        }
    }
}
class SwingChatRoom(val transition: (MatrixState, Boolean) -> Unit, val panel: JPanel, var m: MatrixChatRoom) : SwingState() {
    var inner_scroll_pane = JPanel()
    var c_left = GridBagConstraints()
    var c_right = GridBagConstraints()
    var message_field = JTextField(20)
    init {
        panel.layout = BorderLayout()

        val backfill_button = JButton("Backfill")
        backfill_button.addActionListener({ m.requestBackfill() })
        panel.add(backfill_button, BorderLayout.PAGE_START)

        val group_layout = GroupLayout(inner_scroll_pane)
        inner_scroll_pane.layout = group_layout
        group_layout.autoCreateGaps = true
        group_layout.autoCreateContainerGaps = true
        redrawMessages()
        panel.add(JScrollPane(inner_scroll_pane,
                              JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                  BorderLayout.CENTER)

        val message_panel = JPanel()
        message_panel.layout = BorderLayout()
        var back_button = JButton("Back")
        message_panel.add(back_button, BorderLayout.LINE_START)
        message_panel.add(message_field, BorderLayout.CENTER)
        var send_button = JButton("Send")
        message_panel.add(send_button, BorderLayout.LINE_END)
        panel.add(message_panel, BorderLayout.PAGE_END)
        val onSend: (ActionEvent) -> Unit = {
            val text = message_field.text
            message_field.text = ""
            transition(m.sendMessage(text), true)
        }
        message_field.addActionListener(onSend)
        send_button.addActionListener(onSend)
        back_button.addActionListener({ transition(m.exitRoom(), true) })

    }
    fun redrawMessages() {
        inner_scroll_pane.removeAll()
        val layout = inner_scroll_pane.layout as GroupLayout
        val parallel_group = layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        var seq_vert_groups = layout.createSequentialGroup()
        for (msg in m.messages) {
            val _sender = msg.sender
            val message = msg.message
            val url = msg.url
            val sender = JLabel("$_sender:  ")

            val msg_widget =
            if(url != null) {
                val img = ImageIcon(url);
                val label = JLabel(img)
                label
            } else {
                val message = JTextArea(message)
                message.setEditable(false)
                message.lineWrap = true
                message.wrapStyleWord = true
                message
            }
            parallel_group.addComponent(sender)
            parallel_group.addComponent(msg_widget)
            seq_vert_groups.addComponent(sender)
            seq_vert_groups.addGroup(layout.createSequentialGroup()
                            .addPreferredGap(sender, msg_widget, LayoutStyle.ComponentPlacement.INDENT)
                            .addComponent(msg_widget))

        }
        layout.setHorizontalGroup(parallel_group)
        layout.setVerticalGroup(seq_vert_groups)
    }
    override fun refresh() {
        transition(m.refresh(), true)
    }
    fun update(new_m: MatrixChatRoom) {
        if (m.messages != new_m.messages) {
            m = new_m
            redrawMessages()
        } else {
            m = new_m
        }
    }
}

class App {
    var frame = JFrame("Serif")
    var sstate: SwingState

    init {
        // Each UI will create it's specific DriverFactory
        // And call this function before the backend can get
        // information out of the database
        Database.initDb(DriverFactory())

        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        sstate = constructStateView(MatrixLogin())
        frame.pack()
        frame.setVisible(true)
    }

    fun transition(new_state: MatrixState, partial: Boolean) {
        // TODO: update current view if new_state is the same type as mstate
        val s = sstate
        if (partial) {
            when {
                new_state is MatrixChatRoom && s is SwingChatRoom -> { s.update(new_state); return; }
                new_state is MatrixRooms && s is SwingRooms -> { s.update(new_state); return; }
            }
        }
        sstate = constructStateView(new_state)
    }

    fun constructStateView(mstate: MatrixState): SwingState {
        frame.contentPane.removeAll()
        var panel = JPanel()
        val to_ret = when (mstate) {
            is MatrixLogin -> SwingLogin(::transition, {
                javax.swing.SwingUtilities.invokeLater({
                    sstate.refresh()
                    frame.validate()
                    frame.repaint()
                })
            }, panel, mstate)
            is MatrixRooms -> SwingRooms(::transition, panel, mstate)
            is MatrixChatRoom -> SwingChatRoom(::transition, panel, mstate)
        }
        frame.add(panel)
        frame.validate()
        frame.repaint()
        return to_ret
    }
}

fun main(args: Array<String>) {
    FlatDarkLaf.install()
    UIManager.getLookAndFeelDefaults().put("defaultFont", Font("Serif", Font.PLAIN, 16));
    javax.swing.SwingUtilities.invokeLater({ App() })
}
