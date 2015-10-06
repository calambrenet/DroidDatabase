<h1 id="droiddatabase">DroidDatabase</h1>

<p>DroidDatabase it is a ORM designed to run only on Android. It provides a simple way of working with our SQLite database. <br>
DroidDatabase is in very early stage of development, so it must be used very carefully. They are missing a lot of work to run at 100% and to have all the characteristics of a complete ORM. (See <strong>TODO</strong> section)</p>

<hr>

<h2 id="how-to-use">How to use</h2>

<p>To use this library you just have to download it, or clone, and import it into your Android project. <br>
In Manifest project, you can add the following lines, are optional:</p>

<pre><code>&lt;meta-data android:name="DATABASE_NAME" android:value="community" /&gt;
&lt;meta-data android:name="DATABASE_VERSION" android:value="1" /&gt;
&lt;meta-data android:name="QUERY_LOG" android:value="true" /&gt;
</code></pre>

<p>Now we create our table to the database, it is actually a class (magic):</p>

<blockquote>
  <p>user.class</p>
</blockquote>

<pre><code>@table(name = "user")
public class User implements DatabaseModel{
    @field(type = "integer")
    @primary_key(type = "autoincrement")
    Integer id = null;

    @field(type = "varchar")
    @size(value = 200)
    @notnull(value = true)
    private String _id;

    @field(type = "varchar")
    @size(value = 255)
    @notnull(value = true)
    @unique(value = true)
    private String email;

    @field(type = "varchar")
    @size(value = 255)
    private String name;

    @Override
    public Integer getId() {
        return id;
    }
    @Override
    public void setId(int id) {
        this.id = id;
    }    
    public String get_id() {
        return _id;
    }
    public void set_id(String _id) {
        this._id = _id;
    }    
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }    
    public String getName() {
        return name;
    }    
    public void setName(String name) {
        this.name = name;
    }
}
</code></pre>

<p>At the beginning of the app, before making any calls to the database, you must register the tables and initialize * DroidDatabase *:</p>

<pre><code>Class[] modelList = new Class[]{
    User.class,
};

try {
    DroidDatabase.Register(getContext(), modelList);
} catch (Exception | NotValidDatabaseTableException e) {
    e.printStackTrace();
}
</code></pre>

<p><em>We can now use our database!</em> </p>

<p><strong>Create a new record:</strong> </p>

<pre><code>User user = new User();            
user.set_id("pepe21");
user.setEmail("pepeluis@correo.es");
user.setName("pepe luis");

DroidDatabase db = new DroidDatabase&lt;&gt;(getContext(), user);
User user_saved = null;
try {
    user_saved = (User) db.save()
} catch (Exception e) {
        e.printStackTrace();
        Log.d(TAG, "An error occurred while saving: " + e.getMessage());
} catch (NotNullException e) {
        e.printStackTrace();
        Log.d(TAG, "Null value in a field" + e.getMessage());
}
</code></pre>

<p><strong>Update existing one:</strong> </p>

<pre><code>user_saved.setName("juanito");

db = new DroidDatabase&lt;&gt;(getContext(), user_saved);
try {
    db.save();
} catch (NotNullException e) {
        e.printStackTrace();
}
</code></pre>

<p><strong>Queries:</strong></p>

<pre><code>List&lt;User&gt; table_list = new ArrayList&lt;&gt;();
User table = null;

//search for an item using a filter    
table = new Database&lt;&gt;(getContext(), User.class).filterby("_id", "pepe21", Database.EQUAL).findOne();

//search for an item using several filters
Map&lt;String, String&gt; filter = new HashMap&lt;&gt;();
filter.put("_id", "pepe21");
filter.put("email", "pepeluis@correo.es");
table = new Database&lt;&gt;(getContext(), User.class).filterby(filter, Database.EQUAL).findOne();

//search multiple elements 
table_list = new Database&lt;&gt;(getContext(), User.class).filterby("email", "calambrenet@gmail.com", Database.EQUAL).find();

//remove one or more elements
new Database&lt;&gt;(getContext(), User.class).filterby("_id", "pepe", Database.EQUAL).delete();
</code></pre>

<p></p><h2 id="todo"> TODO</h2> <br>
Things missing, any volunteers?<p></p>

<ul>
<li>Implement system versions when making modifications to the tables.</li>
<li>Relations between tables.</li>
<li>That no fault XD.</li>
<li>Better use of exceptions.</li>
</ul>
